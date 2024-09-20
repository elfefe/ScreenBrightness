package com.elfefe.lowerbrightness

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class OverlayService : Service() {

    lateinit var sharedPreferences: SharedPreferences
    private var dimView: View? = null
    private var windowManager: WindowManager? = null
    private val CHANNEL_ID = "OverlayServiceChannel"
    private var isOverlayEnabled = false
    private var brightnessAlpha = 150 // Default brightness (0-255)
    private var brightnessStep = 51 // Default brightness (0-255)

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(SharedPreferenceKeys.APP_PREFS, MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntentActions(intent)
        startForeground(1, createNotification())

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleIntentActions(intent: Intent?) {
        intent?.let {
            when (it.action) {
                ActionKeys.TOGGLE_OVERLAY -> toggleOverlay()
                ActionKeys.ADJUST_BRIGHTNESS -> adjustBrightness(it.getIntExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha))
                ActionKeys.ADJUST_BRIGHTNESS_STEP -> adjustBrightnessStep(it.getIntExtra(IntentKeys.BRIGHTNESS_STEP, brightnessStep))
                ActionKeys.REDUCE_BRIGHTNESS -> {
                    adjustBrightness(brightnessAlpha + brightnessStep)
//                    sendBrightnessToActivity()
                }
                ActionKeys.INCREASE_BRIGHTNESS -> {
                    adjustBrightness(brightnessAlpha - brightnessStep)
//                    sendBrightnessToActivity()
                }
                "OPEN_APP" -> {
                    val openAppIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(openAppIntent)
                }
            }
        }
    }

    private fun sendBrightnessToActivity() {
        val intent = Intent(IntentKeys.UPDATE_BRIGHTNESS).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha)
        }

        sendBroadcast(intent)
    }

    private fun createOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        dimView = View(this).apply {
            setBackgroundColor(Color.argb(brightnessAlpha, 0, 0, 0))
            setTheme(android.R.style.Theme_Holo_NoActionBar_Fullscreen)
        }

        val params = WindowManager.LayoutParams(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                windowManager?.maximumWindowMetrics?.bounds?.width() ?:
                WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                windowManager?.maximumWindowMetrics?.bounds?.height()?.let { it + 512 } ?:
                WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.MATCH_PARENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        windowManager?.addView(dimView, params)
    }

    private fun removeOverlay() {
        dimView?.let {
            windowManager?.removeView(it)
            dimView = null
        }
    }

    private fun adjustBrightness(alpha: Int) {
        alpha.coerceAtLeast(0).coerceAtMost(255).let {
            sharedPreferences.edit().putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS, it).apply()
            brightnessAlpha = it
            dimView?.setBackgroundColor(Color.argb(it, 0, 0, 0))
        }
    }

    private fun adjustBrightnessStep(alpha: Int) {
        alpha.coerceAtLeast(1).coerceAtMost(85).let {
            sharedPreferences.edit().putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, it).apply()
            brightnessStep = it
        }
    }

    private fun toggleOverlay() {
        if (isOverlayEnabled)
            removeOverlay()
        else createOverlay()
        isOverlayEnabled = !isOverlayEnabled
        updateNotification()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, createNotification())
    }

    private fun createNotification(): Notification {
        // Intents
        val toggleIntent = Intent(this, OverlayService::class.java).apply { action = ActionKeys.TOGGLE_OVERLAY }
        val togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent, pendingIntentFlags())

        val brightnessIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha)
        }
        val brightnessPendingIntent = PendingIntent.getActivity(this, 0, brightnessIntent, pendingIntentFlags())

        val reduceBrightnessIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.REDUCE_BRIGHTNESS
        }
        val reduceBrightnessPendingIntent = PendingIntent.getService(this, 0, reduceBrightnessIntent, PendingIntent.FLAG_MUTABLE)

        val increaseBrightnessIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.INCREASE_BRIGHTNESS
        }
        val increaseBrightnessPendingIntent = PendingIntent.getService(this, 0, increaseBrightnessIntent, PendingIntent.FLAG_MUTABLE)

        // Create notification
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_lower_brightness_monochrome)
            .setProgress(255, 255 - brightnessAlpha, false)
            .addAction(R.drawable.baseline_arrow_left_24, "Reduce", reduceBrightnessPendingIntent)
            .addAction(R.drawable.baseline_arrow_right_24, "Increase", increaseBrightnessPendingIntent)
            .addAction(
                if (isOverlayEnabled) R.drawable.baseline_toggle_off_24
                else R.drawable.baseline_toggle_on_24, "Toggle Overlay", togglePendingIntent)
            .setContentIntent(brightnessPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }


    private fun windowType(): Int =
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_IMMUTABLE

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Overlay Service Channel", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
