package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.RuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores the floating control overlay reliably across device reboots, package updates,
 * and aggressive OEM task removals when the user previously enabled the overlay.
 */
class ServiceRestartReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESTART_OVERLAY = "com.example.ACTION_RESTART_OVERLAY"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != ACTION_RESTART_OVERLAY
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = RuleRepository(context.applicationContext)
                val config = repo.getSnapshot()
                if (config.overlayAutoRestart) {
                    FloatingOverlayService.start(context.applicationContext)
                }
            } catch (_: Exception) {
                // Ignore background restriction failures gracefully on Android 14+ OEM builds
            } finally {
                pendingResult.finish()
            }
        }
    }
}
