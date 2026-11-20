package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartJobApplication
import com.example.data.AssistantRuntimeState
import com.example.data.RuleRepository
import com.example.data.model.AppPreset
import com.example.data.model.HistorySortOption
import com.example.data.model.JobHistoryEntry
import com.example.data.model.JobOutcome
import com.example.data.model.PermissionStatus
import com.example.data.model.RuleConfig
import com.example.engine.EvaluationResult
import com.example.engine.RuleEvaluator
import com.example.engine.ScreenTextParser
import com.example.service.FloatingOverlayService
import com.example.util.PermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MockRideOffer(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val pickupTitle: String,
    val dropTitle: String,
    val fareText: String,
    val distanceText: String,
    val buttonLabel: String,
    val extraNote: String = "Cash / UPI"
) {
    fun screenTextChunks(): List<String> = listOf(
        appName,
        pickupTitle,
        dropTitle,
        fareText,
        distanceText,
        extraNote,
        buttonLabel
    ).filter { it.isNotBlank() }
}

enum class SandboxEvalPhase {
    IDLE,
    SCANNING,
    AUTO_CLICKED,
    SKIPPED,
    NOT_DETECTED,
    MANUAL_ACCEPTED,
    MANUALLY_DISMISSED
}

data class SandboxDialogState(
    val offer: MockRideOffer,
    val phase: SandboxEvalPhase = SandboxEvalPhase.SCANNING,
    val evaluation: EvaluationResult? = null,
    val statusBanner: String = "Scanning visible popup text...",
    val autoClickElapsedMs: Long? = null
)

data class SmartJobUiState(
    val config: RuleConfig = RuleConfig(),
    val isActive: Boolean = false,
    val isAccessibilityConnected: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val notDetectedCount: Int = 0,
    val lastSummary: String = "",
    val permissions: PermissionStatus = PermissionStatus(),
    val allHistory: List<JobHistoryEntry> = emptyList(),
    val filteredHistory: List<JobHistoryEntry> = emptyList(),
    val historyOutcomeFilter: JobOutcome? = null,
    val historySearchQuery: String = "",
    val historySortOption: HistorySortOption = HistorySortOption.NEWEST,
    val sandboxDialog: SandboxDialogState? = null,
    val sandboxAutoEvalEnabled: Boolean = true
)

class SmartJobViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository: RuleRepository = (application as SmartJobApplication).ruleRepository

    private val _permissions = MutableStateFlow(PermissionHelper.checkPermissionStatus(appContext))
    private val _historyOutcomeFilter = MutableStateFlow<JobOutcome?>(null)
    private val _historySearchQuery = MutableStateFlow("")
    private val _historySortOption = MutableStateFlow(HistorySortOption.NEWEST)
    private val _sandboxDialog = MutableStateFlow<SandboxDialogState?>(null)
    private val _sandboxAutoEvalEnabled = MutableStateFlow(true)

    private var sandboxEvalJob: Job? = null
    private var lastSandboxClickElapsedMs: Long = 0L

    private data class RuntimeSnapshot(
        val isActive: Boolean,
        val isAccessibilityConnected: Boolean,
        val isOverlayRunning: Boolean,
        val acceptedCount: Int,
        val skippedCount: Int,
        val notDetectedCount: Int,
        val lastSummary: String
    )

    private val runtimeFlow = combine(
        AssistantRuntimeState.isActive,
        AssistantRuntimeState.isAccessibilityConnected,
        AssistantRuntimeState.isOverlayRunning,
        AssistantRuntimeState.acceptedCount,
        AssistantRuntimeState.skippedCount
    ) { isActive, a11y, overlay, accepted, skipped ->
        RuntimeSnapshot(
            isActive = isActive,
            isAccessibilityConnected = a11y,
            isOverlayRunning = overlay,
            acceptedCount = accepted,
            skippedCount = skipped,
            notDetectedCount = 0,
            lastSummary = ""
        )
    }.combine(
        combine(
            AssistantRuntimeState.notDetectedCount,
            AssistantRuntimeState.lastEvaluatedSummary
        ) { notDetected, summary -> notDetected to summary }
    ) { snapshot, (notDetected, summary) ->
        snapshot.copy(notDetectedCount = notDetected, lastSummary = summary)
    }

    private data class HistoryFilterState(
        val allHistory: List<JobHistoryEntry>,
        val outcomeFilter: JobOutcome?,
        val searchQuery: String,
        val sortOption: HistorySortOption
    )

    private val historyFlow = combine(
        AssistantRuntimeState.jobHistory,
        _historyOutcomeFilter,
        _historySearchQuery,
        _historySortOption
    ) { history, outcomeFilter, searchQuery, sortOption ->
        HistoryFilterState(history, outcomeFilter, searchQuery, sortOption)
    }

    val uiState: StateFlow<SmartJobUiState> = combine(
        repository.ruleConfigFlow,
        runtimeFlow,
        _permissions,
        historyFlow,
        combine(_sandboxDialog, _sandboxAutoEvalEnabled) { dialog, autoEval -> dialog to autoEval }
    ) { config, runtime, permissions, historyState, (sandboxDialog, sandboxAutoEval) ->
        val filtered = applyHistoryFilterAndSort(
            entries = historyState.allHistory,
            outcomeFilter = historyState.outcomeFilter,
            query = historyState.searchQuery,
            sortOption = historyState.sortOption
        )
        SmartJobUiState(
            config = config,
            isActive = runtime.isActive,
            isAccessibilityConnected = runtime.isAccessibilityConnected,
            isOverlayRunning = runtime.isOverlayRunning,
            acceptedCount = runtime.acceptedCount,
            skippedCount = runtime.skippedCount,
            notDetectedCount = runtime.notDetectedCount,
            lastSummary = runtime.lastSummary,
            permissions = permissions,
            allHistory = historyState.allHistory,
            filteredHistory = filtered,
            historyOutcomeFilter = historyState.outcomeFilter,
            historySearchQuery = historyState.searchQuery,
            historySortOption = historyState.sortOption,
            sandboxDialog = sandboxDialog,
            sandboxAutoEvalEnabled = sandboxAutoEval
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SmartJobUiState(
            config = AssistantRuntimeState.latestConfig.value,
            permissions = PermissionHelper.checkPermissionStatus(appContext)
        )
    )

    fun refreshPermissions() {
        _permissions.value = PermissionHelper.checkPermissionStatus(appContext)
    }

    fun toggleAssistantActive() {
        AssistantRuntimeState.toggleActive()
    }

    fun startOrStopOverlay(): Boolean {
        if (AssistantRuntimeState.isOverlayRunning.value) {
            FloatingOverlayService.stop(appContext)
            return true
        }
        refreshPermissions()
        if (!_permissions.value.overlayEnabled) {
            PermissionHelper.openOverlaySettings(appContext)
            return false
        }
        FloatingOverlayService.start(appContext)
        return true
    }

    // --- Rule Persistence Updates ---

    fun selectPreset(preset: AppPreset) {
        viewModelScope.launch { repository.applyPreset(preset) }
    }

    fun updateTargetPackage(pkg: String) {
        viewModelScope.launch { repository.updateTargetPackage(pkg) }
    }

    fun updateActionKeywords(keywords: String) {
        viewModelScope.launch { repository.updateActionKeywords(keywords) }
    }

    fun updateMinFareEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateMinFareEnabled(enabled) }
    }

    fun updateMinFareAmount(amount: Double) {
        viewModelScope.launch { repository.updateMinFareAmount(amount) }
    }

    fun updateMaxDistanceEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateMaxDistanceEnabled(enabled) }
    }

    fun updateMaxDistanceKm(km: Double) {
        viewModelScope.launch { repository.updateMaxDistanceKm(km) }
    }

    fun updateMinPerKmEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateMinPerKmEnabled(enabled) }
    }

    fun updateMinPerKmRate(rate: Double) {
        viewModelScope.launch { repository.updateMinPerKmRate(rate) }
    }

    fun updateSkipIfUndetected(skip: Boolean) {
        viewModelScope.launch { repository.updateSkipIfUndetected(skip) }
    }

    fun updateReactionDelayMs(delayMs: Long) {
        viewModelScope.launch { repository.updateReactionDelayMs(delayMs) }
    }

    fun updateCooldownMs(cooldownMs: Long) {
        viewModelScope.launch { repository.updateCooldownMs(cooldownMs) }
    }

    fun toggleDarkTheme() {
        val next = !uiState.value.config.darkThemeEnabled
        viewModelScope.launch { repository.updateDarkTheme(next) }
    }

    // --- History Filtering & Sorting ---

    fun setHistoryOutcomeFilter(outcome: JobOutcome?) {
        _historyOutcomeFilter.value = outcome
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistorySortOption(option: HistorySortOption) {
        _historySortOption.value = option
    }

    fun clearHistory() {
        AssistantRuntimeState.clearHistory()
    }

    private fun applyHistoryFilterAndSort(
        entries: List<JobHistoryEntry>,
        outcomeFilter: JobOutcome?,
        query: String,
        sortOption: HistorySortOption
    ): List<JobHistoryEntry> {
        val trimmed = query.trim().lowercase()
        val filtered = entries.filter { entry ->
            val matchesOutcome = outcomeFilter == null || entry.outcome == outcomeFilter
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                entry.sourceLabel.lowercase().contains(trimmed) ||
                    entry.sourcePackage.lowercase().contains(trimmed) ||
                    entry.outcome.label.lowercase().contains(trimmed) ||
                    entry.summaryLine.lowercase().contains(trimmed) ||
                    (entry.matchedKeyword?.lowercase()?.contains(trimmed) == true) ||
                    entry.triggeredRules.any { it.lowercase().contains(trimmed) } ||
                    entry.rawSnippet.lowercase().contains(trimmed)
            }
            matchesOutcome && matchesQuery
        }

        return when (sortOption) {
            HistorySortOption.NEWEST -> filtered.sortedByDescending { it.timestampMs }
            HistorySortOption.OLDEST -> filtered.sortedBy { it.timestampMs }
            HistorySortOption.HIGHEST_FARE -> filtered.sortedByDescending { it.fareAmount ?: -1.0 }
            HistorySortOption.SHORTEST_DISTANCE -> filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            HistorySortOption.BEST_RATE -> filtered.sortedByDescending { it.ratePerKm ?: -1.0 }
        }
    }

    // --- Sandbox Simulation ---

    fun setSandboxAutoEvalEnabled(enabled: Boolean) {
        _sandboxAutoEvalEnabled.value = enabled
    }

    fun spawnMockOffer(offer: MockRideOffer) {
        sandboxEvalJob?.cancel()
        AssistantRuntimeState.setSandboxDialogVisible(true)

        val autoEval = _sandboxAutoEvalEnabled.value
        _sandboxDialog.value = SandboxDialogState(
            offer = offer,
            phase = if (autoEval) SandboxEvalPhase.SCANNING else SandboxEvalPhase.IDLE,
            statusBanner = if (autoEval) {
                "Parsing popup nodes with active SmartJobHelper rules..."
            } else {
                "Auto-evaluator paused — tap Accept or Dismiss manually"
            }
        )

        if (!autoEval) return

        val config = uiState.value.config
        val initialHistorySize = AssistantRuntimeState.jobHistory.value.size

        sandboxEvalJob = viewModelScope.launch {
            // Brief visual scan beat so the user can watch the rule engine inspect the card
            delay(260L)

            // If the live Android AccessibilityService already clicked this dialog, avoid double-logging
            val currentDialog = _sandboxDialog.value
            if (currentDialog == null || currentDialog.offer.id != offer.id) return@launch
            if (AssistantRuntimeState.jobHistory.value.size > initialHistorySize &&
                AssistantRuntimeState.isAccessibilityConnected.value &&
                AssistantRuntimeState.isActive.value
            ) {
                val latestEntry = AssistantRuntimeState.jobHistory.value.firstOrNull()
                if (latestEntry != null) {
                    _sandboxDialog.value = currentDialog.copy(
                        phase = when (latestEntry.outcome) {
                            JobOutcome.ACCEPTED -> SandboxEvalPhase.AUTO_CLICKED
                            JobOutcome.SKIPPED -> SandboxEvalPhase.SKIPPED
                            JobOutcome.NOT_DETECTED -> SandboxEvalPhase.NOT_DETECTED
                        },
                        statusBanner = "Live AccessibilityService: ${latestEntry.summaryLine}"
                    )
                    return@launch
                }
            }

            val chunks = offer.screenTextChunks()
            val metrics = ScreenTextParser.parse(chunks)
            val matchedKw = ScreenTextParser.findMatchingKeyword(
                candidateText = offer.buttonLabel,
                keywords = config.parsedKeywords()
            )
            val evaluation = RuleEvaluator.evaluate(
                metrics = metrics,
                matchedKeyword = matchedKw,
                config = config
            )

            if (evaluation.shouldClick) {
                val now = SystemClock.elapsedRealtime()
                val elapsedSinceLast = now - lastSandboxClickElapsedMs
                if (elapsedSinceLast < config.cooldownMs) {
                    val waitMs = config.cooldownMs - elapsedSinceLast
                    _sandboxDialog.value = currentDialog.copy(
                        evaluation = evaluation,
                        statusBanner = "Cooldown guard active (${waitMs}ms remaining)..."
                    )
                    delay(waitMs)
                }

                if (config.reactionDelayMs > 0L) {
                    _sandboxDialog.value = _sandboxDialog.value?.copy(
                        evaluation = evaluation,
                        statusBanner = "Matched! Waiting ${config.reactionDelayMs}ms reaction delay..."
                    )
                    delay(config.reactionDelayMs)
                }

                if (_sandboxDialog.value?.offer?.id != offer.id) return@launch
                lastSandboxClickElapsedMs = SystemClock.elapsedRealtime()

                AssistantRuntimeState.recordJobEvent(
                    JobHistoryEntry(
                        sourcePackage = offer.packageName,
                        sourceLabel = "${offer.appName} (Sandbox)",
                        outcome = JobOutcome.ACCEPTED,
                        fareAmount = evaluation.fareAmount,
                        distanceKm = evaluation.distanceKm,
                        ratePerKm = evaluation.ratePerKm,
                        matchedKeyword = evaluation.matchedKeyword,
                        triggeredRules = evaluation.triggeredRules + "Method: SANDBOX_SIM",
                        summaryLine = evaluation.summaryLine,
                        clickMethod = "SANDBOX_SIM",
                        rawSnippet = chunks.joinToString(" • ")
                    )
                )

                _sandboxDialog.value = _sandboxDialog.value?.copy(
                    phase = SandboxEvalPhase.AUTO_CLICKED,
                    evaluation = evaluation,
                    statusBanner = "Auto-clicked \"${offer.buttonLabel}\" (${config.reactionDelayMs}ms delay)",
                    autoClickElapsedMs = config.reactionDelayMs
                )
            } else {
                AssistantRuntimeState.recordJobEvent(
                    JobHistoryEntry(
                        sourcePackage = offer.packageName,
                        sourceLabel = "${offer.appName} (Sandbox)",
                        outcome = evaluation.outcome,
                        fareAmount = evaluation.fareAmount,
                        distanceKm = evaluation.distanceKm,
                        ratePerKm = evaluation.ratePerKm,
                        matchedKeyword = evaluation.matchedKeyword,
                        triggeredRules = evaluation.triggeredRules,
                        summaryLine = evaluation.summaryLine,
                        clickMethod = null,
                        rawSnippet = chunks.joinToString(" • ")
                    )
                )

                val nextPhase = if (evaluation.outcome == JobOutcome.NOT_DETECTED) {
                    SandboxEvalPhase.NOT_DETECTED
                } else {
                    SandboxEvalPhase.SKIPPED
                }
                _sandboxDialog.value = _sandboxDialog.value?.copy(
                    phase = nextPhase,
                    evaluation = evaluation,
                    statusBanner = evaluation.summaryLine
                )
            }
        }
    }

    fun onSandboxAcceptTapped() {
        val current = _sandboxDialog.value ?: return
        sandboxEvalJob?.cancel()
        if (current.phase != SandboxEvalPhase.AUTO_CLICKED) {
            _sandboxDialog.value = current.copy(
                phase = SandboxEvalPhase.MANUAL_ACCEPTED,
                statusBanner = "Button \"${current.offer.buttonLabel}\" clicked!"
            )
        }
    }

    fun dismissSandboxDialog() {
        sandboxEvalJob?.cancel()
        AssistantRuntimeState.setSandboxDialogVisible(false)
        _sandboxDialog.value = null
    }
}
