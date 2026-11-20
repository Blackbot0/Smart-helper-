package com.example

import android.app.Application
import com.example.data.AssistantRuntimeState
import com.example.data.RuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SmartJobApplication : Application() {

    lateinit var ruleRepository: RuleRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ruleRepository = RuleRepository(applicationContext)

        // Keep process-wide runtime config synchronized with DataStore
        // so AccessibilityService and FloatingOverlayService always have fresh rules
        // even after background OEM process restarts.
        appScope.launch {
            ruleRepository.ruleConfigFlow.collectLatest { config ->
                AssistantRuntimeState.updateLatestConfig(config)
            }
        }
    }
}
