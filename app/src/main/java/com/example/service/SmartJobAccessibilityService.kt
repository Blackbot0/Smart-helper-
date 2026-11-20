package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.SmartJobApplication
import com.example.data.AssistantRuntimeState
import com.example.data.model.AppPreset
import com.example.data.model.JobHistoryEntry
import com.example.data.model.JobOutcome
import com.example.data.model.RuleConfig
import com.example.engine.RuleEvaluator
import com.example.engine.ScreenTextParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time screen text parser and rule-driven auto-clicker AccessibilityService.
 * Starts PAUSED by default (controlled via AssistantRuntimeState.isActive).
 */
class SmartJobAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var lastClickTimestampMs: Long = 0L

    @Volatile
    private var lastEvaluatedFingerprint: String = ""

    @Volatile
    private var lastEvaluatedFingerprintTimeMs: Long = 0L

    private var pendingClickJob: Job? = null

    private data class CandidateButton(
        val node: AccessibilityNodeInfo,
        val matchedKeyword: String,
        val bounds: Rect
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        AssistantRuntimeState.setAccessibilityConnected(true)

        // Reload persisted DataStore rules immediately in case the OS restarted the
        // service in the background after an OEM process kill.
        val app = application as? SmartJobApplication
        if (app != null) {
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val snapshot = app.ruleRepository.getSnapshot()
                    AssistantRuntimeState.updateLatestConfig(snapshot)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!AssistantRuntimeState.isActive.value) return

        val config = AssistantRuntimeState.latestConfig.value
        val eventPackage = event.packageName?.toString().orEmpty()
        if (eventPackage.isBlank()) return

        // Ignore Android System UI overlays and keyboards
        if (eventPackage == "com.android.systemui" || eventPackage.contains("inputmethod")) {
            return
        }

        val isOwnApp = eventPackage == packageName
        if (isOwnApp && !AssistantRuntimeState.isSandboxDialogVisible.value) {
            return
        }

        // Apply configured package filter unless Custom/All (blank) or testing in Sandbox
        if (!isOwnApp && config.targetPackageName.isNotBlank()) {
            if (!eventPackage.equals(config.targetPackageName, ignoreCase = true)) {
                return
            }
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTimestampMs < config.cooldownMs) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val collectedTexts = mutableListOf<String>()
        val keywords = config.parsedKeywords()
        var matchedButton: CandidateButton? = null

        try {
            traverseHierarchy(
                node = rootNode,
                keywords = keywords,
                collectedTexts = collectedTexts,
                onButtonFound = { candidate ->
                    if (matchedButton == null) {
                        matchedButton = candidate
                    }
                }
            )

            if (collectedTexts.isEmpty()) return

            // Only evaluate screens that contain a target keyword button or financial/distance offer cues
            val parsedMetrics = ScreenTextParser.parse(collectedTexts)
            val button = matchedButton
            if (button == null && parsedMetrics.primaryFare == null && parsedMetrics.primaryDistanceKm == null) {
                return
            }

            // Deduplicate rapid window content updates for the identical offer popup
            val fingerprint = buildFingerprint(
                pkg = eventPackage,
                fare = parsedMetrics.primaryFare,
                distance = parsedMetrics.primaryDistanceKm,
                keyword = button?.matchedKeyword,
                textCount = collectedTexts.size
            )
            if (fingerprint == lastEvaluatedFingerprint &&
                now - lastEvaluatedFingerprintTimeMs < config.cooldownMs.coerceAtLeast(2500L)
            ) {
                return
            }

            // If we didn't find a target keyword button on a normal non-offer screen, avoid log noise
            if (button == null) {
                return
            }

            lastEvaluatedFingerprint = fingerprint
            lastEvaluatedFingerprintTimeMs = now

            val evaluation = RuleEvaluator.evaluate(
                metrics = parsedMetrics,
                matchedKeyword = button.matchedKeyword,
                config = config
            )

            val snippet = collectedTexts.take(8).joinToString(" • ").take(180)
            val sourceLabel = if (isOwnApp) "Sandbox" else AppPreset.labelForPackage(eventPackage)

            if (!evaluation.shouldClick) {
                AssistantRuntimeState.recordJobEvent(
                    JobHistoryEntry(
                        sourcePackage = eventPackage,
                        sourceLabel = sourceLabel,
                        outcome = evaluation.outcome,
                        fareAmount = evaluation.fareAmount,
                        distanceKm = evaluation.distanceKm,
                        ratePerKm = evaluation.ratePerKm,
                        matchedKeyword = evaluation.matchedKeyword,
                        triggeredRules = evaluation.triggeredRules,
                        summaryLine = evaluation.summaryLine,
                        clickMethod = null,
                        rawSnippet = snippet
                    )
                )
                return
            }

            // Reserve cooldown slot immediately so overlapping events cannot double-click
            lastClickTimestampMs = now
            val targetBounds = Rect(button.bounds)
            val clickableNode = findClickableTarget(button.node)

            scheduleClick(
                config = config,
                clickableNode = clickableNode,
                targetBounds = targetBounds,
                eventPackage = eventPackage,
                sourceLabel = sourceLabel,
                evaluation = evaluation,
                snippet = snippet
            )
        } catch (_: Exception) {
            // Gracefully handle stale AccessibilityNodeInfo trees during rapid window transitions
        }
    }

    private fun scheduleClick(
        config: RuleConfig,
        clickableNode: AccessibilityNodeInfo?,
        targetBounds: Rect,
        eventPackage: String,
        sourceLabel: String,
        evaluation: com.example.engine.EvaluationResult,
        snippet: String
    ) {
        pendingClickJob?.cancel()
        pendingClickJob = serviceScope.launch {
            if (config.reactionDelayMs > 0L) {
                delay(config.reactionDelayMs)
            }
            if (!AssistantRuntimeState.isActive.value) return@launch

            var clickMethod: String? = null
            val clickedViaNode = try {
                clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            } catch (_: Exception) {
                false
            }

            if (clickedViaNode) {
                clickMethod = "ACTION_CLICK"
            } else if (!targetBounds.isEmpty) {
                val dispatched = dispatchCenterTapGesture(targetBounds)
                if (dispatched) {
                    clickMethod = "GESTURE_TAP"
                }
            }

            val finalOutcome = if (clickMethod != null) JobOutcome.ACCEPTED else JobOutcome.SKIPPED
            val finalRules = if (clickMethod != null) {
                evaluation.triggeredRules + "Method: $clickMethod"
            } else {
                evaluation.triggeredRules + "Click dispatch failed"
            }
            val finalSummary = if (clickMethod != null) {
                evaluation.summaryLine
            } else {
                evaluation.summaryLine.replace("CLICKED", "Click Failed")
            }

            AssistantRuntimeState.recordJobEvent(
                JobHistoryEntry(
                    sourcePackage = eventPackage,
                    sourceLabel = sourceLabel,
                    outcome = finalOutcome,
                    fareAmount = evaluation.fareAmount,
                    distanceKm = evaluation.distanceKm,
                    ratePerKm = evaluation.ratePerKm,
                    matchedKeyword = evaluation.matchedKeyword,
                    triggeredRules = finalRules,
                    summaryLine = finalSummary,
                    clickMethod = clickMethod,
                    rawSnippet = snippet
                )
            )
        }
    }

    private fun traverseHierarchy(
        node: AccessibilityNodeInfo?,
        keywords: List<String>,
        collectedTexts: MutableList<String>,
        onButtonFound: (CandidateButton) -> Unit,
        depth: Int = 0
    ) {
        if (node == null || depth > 24) return
        if (!node.isVisibleToUser) return

        val nodeText = node.text?.toString()?.trim().orEmpty()
        val contentDesc = node.contentDescription?.toString()?.trim().orEmpty()

        if (nodeText.isNotEmpty()) {
            collectedTexts.add(nodeText)
        }
        if (contentDesc.isNotEmpty() && !contentDesc.equals(nodeText, ignoreCase = true)) {
            collectedTexts.add(contentDesc)
        }

        val combinedLabel = when {
            nodeText.isNotEmpty() -> nodeText
            contentDesc.isNotEmpty() -> contentDesc
            else -> ""
        }

        if (combinedLabel.isNotEmpty()) {
            val matchedKw = ScreenTextParser.findMatchingKeyword(combinedLabel, keywords)
            if (matchedKw != null && (node.isClickable || findClickableTarget(node) != null || isLikelyActionButton(node))) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    onButtonFound(CandidateButton(node = node, matchedKeyword = matchedKw, bounds = rect))
                }
            }
        }

        val count = node.childCount
        for (i in 0 until count) {
            traverseHierarchy(
                node = node.getChild(i),
                keywords = keywords,
                collectedTexts = collectedTexts,
                onButtonFound = onButtonFound,
                depth = depth + 1
            )
        }
    }

    private fun isLikelyActionButton(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString().orEmpty()
        return className.contains("Button", ignoreCase = true) ||
            className.contains("TextView", ignoreCase = true)
    }

    private fun findClickableTarget(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops < 5) {
            if (current.isClickable && current.isEnabled) {
                return current
            }
            current = current.parent
            hops++
        }
        return null
    }

    private fun dispatchCenterTapGesture(bounds: Rect): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val cx = bounds.exactCenterX().coerceAtLeast(1f)
        val cy = bounds.exactCenterY().coerceAtLeast(1f)
        val path = Path().apply { moveTo(cx, cy) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 45L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun buildFingerprint(
        pkg: String,
        fare: Double?,
        distance: Double?,
        keyword: String?,
        textCount: Int
    ): String {
        return "$pkg|${fare ?: "na"}|${distance ?: "na"}|${keyword ?: "na"}|$textCount"
    }

    override fun onInterrupt() {
        pendingClickJob?.cancel()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AssistantRuntimeState.setAccessibilityConnected(false)
        AssistantRuntimeState.setActive(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AssistantRuntimeState.setAccessibilityConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }
}
