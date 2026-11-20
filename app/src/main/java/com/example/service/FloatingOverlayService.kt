package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SmartJobApplication
import com.example.data.AssistantRuntimeState
import com.example.data.model.RuleConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground Service (FOREGROUND_SERVICE_SPECIAL_USE) hosting a compact, draggable,
 * edge-snapping floating status pill over other apps.
 * Single tap toggles ACTIVE/PAUSED; drag repositions smoothly; long-press opens dashboard.
 */
class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.overlay.START"
        const val ACTION_STOP = "com.example.overlay.STOP"
        const val ACTION_TOGGLE_ACTIVE = "com.example.overlay.TOGGLE_ACTIVE"

        private const val CHANNEL_ID = "smart_job_overlay_channel"
        private const val NOTIFICATION_ID = 4021

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var windowManager: WindowManager
    private var overlayView: LinearLayout? = null
    private var statusDotView: View? = null
    private var statusLabelView: TextView? = null
    private var counterBadgeView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var explicitlyStopped = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                explicitlyStopped = true
                persistOverlayPreference(false)
                AssistantRuntimeState.setActive(false)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_ACTIVE -> {
                AssistantRuntimeState.toggleActive()
                updateNotification()
                return START_STICKY
            }
        }

        explicitlyStopped = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AssistantRuntimeState.setOverlayRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        promoteToForeground()
        persistOverlayPreference(true)

        if (overlayView == null) {
            attachFloatingPill()
            observeRuntimeState()
        }

        AssistantRuntimeState.setOverlayRunning(true)
        return START_STICKY
    }

    private fun promoteToForeground() {
        val notification = buildNotification(
            isActive = AssistantRuntimeState.isActive.value,
            config = AssistantRuntimeState.latestConfig.value,
            acceptedCount = AssistantRuntimeState.acceptedCount.value
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeRuntimeState() {
        serviceScope.launch {
            combine(
                AssistantRuntimeState.isActive,
                AssistantRuntimeState.latestConfig,
                AssistantRuntimeState.acceptedCount
            ) { isActive, config, acceptedCount ->
                Triple(isActive, config, acceptedCount)
            }.collect { (isActive, config, acceptedCount) ->
                renderPillState(isActive, config, acceptedCount)
                updateNotification(isActive, config, acceptedCount)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachFloatingPill() {
        val density = resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        val pillContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            elevation = dp(8).toFloat()
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                marginEnd = dp(8)
            }
        }

        val label = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = 0.03f
            isSingleLine = true
        }

        val badge = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(2), dp(7), dp(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(8)
            }
        }

        pillContainer.addView(dot)
        pillContainer.addView(label)
        pillContainer.addView(badge)

        statusDotView = dot
        statusLabelView = label
        counterBadgeView = badge
        overlayView = pillContainer

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(180)
        }
        layoutParams = params

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var downTime = 0L
        var isDragging = false

        pillContainer.setOnTouchListener { view, event ->
            val currentParams = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    downTime = SystemClock.elapsedRealtime()
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        val maxX = (metrics.widthPixels - view.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)
                        currentParams.x = (initialX + dx).coerceIn(0, maxX)
                        currentParams.y = (initialY + dy).coerceIn(dp(28), maxY)
                        runCatching { windowManager.updateViewLayout(view, currentParams) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val elapsed = SystemClock.elapsedRealtime() - downTime
                    if (!isDragging) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (elapsed >= 550L) {
                            openMainDashboard()
                        } else {
                            AssistantRuntimeState.toggleActive()
                        }
                    } else {
                        snapToNearestEdge(view, currentParams)
                    }
                    true
                }

                else -> false
            }
        }

        renderPillState(
            isActive = AssistantRuntimeState.isActive.value,
            config = AssistantRuntimeState.latestConfig.value,
            acceptedCount = AssistantRuntimeState.acceptedCount.value
        )

        runCatching {
            windowManager.addView(pillContainer, params)
        }
    }

    private fun snapToNearestEdge(view: View, params: WindowManager.LayoutParams) {
        val density = resources.displayMetrics.density
        val edgeMargin = (10 * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val midX = params.x + view.width / 2
        val targetX = if (midX < screenWidth / 2) {
            edgeMargin
        } else {
            (screenWidth - view.width - edgeMargin).coerceAtLeast(edgeMargin)
        }

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                if (overlayView != null) {
                    runCatching { windowManager.updateViewLayout(view, params) }
                }
            }
            start()
        }
    }

    private fun renderPillState(isActive: Boolean, config: RuleConfig, acceptedCount: Int) {
        val density = resources.displayMetrics.density
        val cornerRadius = 24f * density

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            if (isActive) {
                setColor(Color.parseColor("#059669")) // Vibrant Emerald
                setStroke((1.5f * density).toInt(), Color.parseColor("#6EE7B7"))
            } else {
                setColor(Color.parseColor("#1E293B")) // Dark Slate
                setStroke((1.5f * density).toInt(), Color.parseColor("#475569"))
            }
        }

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                if (isActive) Color.parseColor("#A7F3D0") else Color.parseColor("#94A3B8")
            )
        }

        val badgeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = 12f * density
            setColor(
                if (isActive) Color.parseColor("#064E3B") else Color.parseColor("#0F172A")
            )
        }

        overlayView?.background = bgDrawable
        statusDotView?.background = dotDrawable
        statusLabelView?.text = if (isActive) {
            "ACTIVE (${config.activePillRuleTag()})"
        } else {
            "PAUSED"
        }
        counterBadgeView?.apply {
            background = badgeDrawable
            text = acceptedCount.toString()
        }
    }

    private fun openMainDashboard() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(launchIntent) }
    }

    private fun updateNotification(
        isActive: Boolean = AssistantRuntimeState.isActive.value,
        config: RuleConfig = AssistantRuntimeState.latestConfig.value,
        acceptedCount: Int = AssistantRuntimeState.acceptedCount.value
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(isActive, config, acceptedCount))
    }

    private fun buildNotification(
        isActive: Boolean,
        config: RuleConfig,
        acceptedCount: Int
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, FloatingOverlayService::class.java).apply {
                action = ACTION_TOGGLE_ACTIVE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            12,
            Intent(this, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (isActive) {
            "ACTIVE (${config.activePillRuleTag()}) • $acceptedCount Accepted"
        } else {
            "PAUSED • $acceptedCount Accepted"
        }
        val contentText = if (isActive) {
            getString(R.string.overlay_notification_running)
        } else {
            getString(R.string.overlay_notification_paused)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(statusTitle)
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                if (isActive) "Pause" else "Activate",
                toggleIntent
            )
            .addAction(
                0,
                getString(R.string.overlay_action_stop),
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun persistOverlayPreference(enabled: Boolean) {
        val app = application as? SmartJobApplication ?: return
        serviceScope.launch(Dispatchers.IO) {
            runCatching { app.ruleRepository.updateOverlayAutoRestart(enabled) }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Guard against aggressive OEM swipe-from-recents kills when overlay was active
        if (!explicitlyStopped && AssistantRuntimeState.latestConfig.value.overlayAutoRestart) {
            val restartIntent = Intent(this, ServiceRestartReceiver::class.java).apply {
                action = ServiceRestartReceiver.ACTION_RESTART_OVERLAY
            }
            val pending = PendingIntent.getBroadcast(
                this,
                77,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 2000L,
                pending
            )
        }
    }

    override fun onDestroy() {
        AssistantRuntimeState.setOverlayRunning(false)
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
