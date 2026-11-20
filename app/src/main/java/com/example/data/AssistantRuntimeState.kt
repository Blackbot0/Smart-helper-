package com.example.data

import com.example.data.model.JobHistoryEntry
import com.example.data.model.JobOutcome
import com.example.data.model.RuleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide reactive bridge shared across AccessibilityService,
 * FloatingOverlayService, and Jetpack Compose UI.
 * Starts PAUSED by default per safety requirements.
 */
object AssistantRuntimeState {
    private const val MAX_HISTORY_ITEMS = 500

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isSandboxDialogVisible = MutableStateFlow(false)
    val isSandboxDialogVisible: StateFlow<Boolean> = _isSandboxDialogVisible.asStateFlow()

    private val _latestConfig = MutableStateFlow(RuleConfig())
    val latestConfig: StateFlow<RuleConfig> = _latestConfig.asStateFlow()

    private val _jobHistory = MutableStateFlow<List<JobHistoryEntry>>(emptyList())
    val jobHistory: StateFlow<List<JobHistoryEntry>> = _jobHistory.asStateFlow()

    private val _acceptedCount = MutableStateFlow(0)
    val acceptedCount: StateFlow<Int> = _acceptedCount.asStateFlow()

    private val _skippedCount = MutableStateFlow(0)
    val skippedCount: StateFlow<Int> = _skippedCount.asStateFlow()

    private val _notDetectedCount = MutableStateFlow(0)
    val notDetectedCount: StateFlow<Int> = _notDetectedCount.asStateFlow()

    private val _lastEvaluatedSummary = MutableStateFlow("Standing by — assistant is paused")
    val lastEvaluatedSummary: StateFlow<String> = _lastEvaluatedSummary.asStateFlow()

    fun setActive(active: Boolean) {
        _isActive.value = active
        _lastEvaluatedSummary.value = if (active) {
            "Monitoring active (${_latestConfig.value.activePillRuleTag()})"
        } else {
            "Assistant paused"
        }
    }

    fun toggleActive(): Boolean {
        val next = !_isActive.value
        setActive(next)
        return next
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun setSandboxDialogVisible(visible: Boolean) {
        _isSandboxDialogVisible.value = visible
    }

    fun updateLatestConfig(config: RuleConfig) {
        _latestConfig.value = config
    }

    fun recordJobEvent(entry: JobHistoryEntry) {
        _jobHistory.update { current ->
            (listOf(entry) + current).take(MAX_HISTORY_ITEMS)
        }
        when (entry.outcome) {
            JobOutcome.ACCEPTED -> _acceptedCount.update { it + 1 }
            JobOutcome.SKIPPED -> _skippedCount.update { it + 1 }
            JobOutcome.NOT_DETECTED -> _notDetectedCount.update { it + 1 }
        }
        _lastEvaluatedSummary.value = entry.summaryLine
    }

    fun clearHistory() {
        _jobHistory.value = emptyList()
        _acceptedCount.value = 0
        _skippedCount.value = 0
        _notDetectedCount.value = 0
        _lastEvaluatedSummary.value = if (_isActive.value) {
            "Log cleared — monitoring active"
        } else {
            "Log cleared — assistant paused"
        }
    }
}
