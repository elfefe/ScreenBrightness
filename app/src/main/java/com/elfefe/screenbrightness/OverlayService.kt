package com.elfefe.screenbrightness

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class OverlayService : Service() {

    lateinit var sharedPreferences: SharedPreferences
    private var dimView: View? = null
    private var windowManager: WindowManager? = null
    private var isOverlayEnabled = false
    private var brightnessAlpha = 150 // Default brightness (0-255)
    private var brightnessStep = 51 // Default brightness (0-255)
    private var color = com.elfefe.screenbrightness.Color.fromColor(androidx.compose.ui.graphics.Color.Black) // Default color

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(SharedPreferenceKeys.APP_PREFS, MODE_PRIVATE)

        brightnessAlpha = sharedPreferences.getInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS, brightnessAlpha)
        brightnessStep = sharedPreferences.getInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, brightnessStep)
        color = com.elfefe.screenbrightness.Color.fromLong(
            sharedPreferences.getLong(SharedPreferenceKeys.CURRENT_COLOR, color.toLong()))
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
                ActionKeys.ACTION_START_OVERLAY -> showOverlay()
                ActionKeys.ACTION_STOP_OVERLAY -> hideOverlay()
                ActionKeys.TOGGLE_OVERLAY -> toggleOverlay()
                ActionKeys.ADJUST_BRIGHTNESS -> adjustBrightness(it.getIntExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha))
                ActionKeys.ADJUST_BRIGHTNESS_STEP -> adjustBrightnessStep(it.getIntExtra(IntentKeys.BRIGHTNESS_STEP, brightnessStep))
                ActionKeys.REDUCE_BRIGHTNESS -> {
                    adjustBrightness(brightnessAlpha + brightnessStep)
                    // TODO: Allow communication from OverlayService to MainActivity
//                    sendBrightnessToActivity()
                }
                ActionKeys.INCREASE_BRIGHTNESS -> {
                    adjustBrightness(brightnessAlpha - brightnessStep)
//                    sendBrightnessToActivity()
                }
                ActionKeys.ADJUST_COLOR -> adjustColor(it.getLongExtra(IntentKeys.UPDATE_COLOR, color.toLong()))
                "OPEN_APP" -> {
                    val openAppIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(openAppIntent)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        removeOverlay()
        createOverlay()
    }

    private fun sendBrightnessToActivity() {
        val intent = Intent(IntentKeys.UPDATE_BRIGHTNESS).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha)
        }

        sendBroadcast(intent)
    }

    private fun createOverlay() {
        if (dimView != null) removeOverlay()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        dimView = View(this).apply {
            setTheme(android.R.style.Theme_Holo_NoActionBar_Fullscreen)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundTintBlendMode = BlendMode.HARD_LIGHT
                setLayerType(View.LAYER_TYPE_SOFTWARE, Paint().apply {
                    blendMode = BlendMode.HARD_LIGHT
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                })
            }
        }

        setOverlayColor()

        dimView?.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
                return@setOnApplyWindowInsetsListener insets

            val params = dimView?.layoutParams
            params?.height = windowManager?.maximumWindowMetrics?.bounds?.height()?.let {
                it + insets.systemWindowInsetTop
            } ?: WindowManager.LayoutParams.MATCH_PARENT
            dimView?.layoutParams = params
            dimView?.invalidate()
            dimView?.requestLayout()
            insets
        }

        val params = WindowManager.LayoutParams(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                windowManager?.maximumWindowMetrics?.bounds?.width() ?:
                WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.dimAmount = 1f

        windowManager?.addView(dimView, params)

        sharedPreferences.edit().putBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, true).apply()
    }

    private fun removeOverlay() {
        dimView?.let {
            windowManager?.removeView(it)
            dimView = null
        }

        sharedPreferences.edit().putBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, false).apply()
    }

    private fun adjustBrightness(alpha: Int) {
        alpha.coerceAtLeast(0).coerceAtMost(255).let {
            sharedPreferences.edit().putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS, it).apply()
            brightnessAlpha = it
            setOverlayColor()
        }
    }

    private fun adjustBrightnessStep(alpha: Int) {
        alpha.coerceAtLeast(1).coerceAtMost(85).let {
            sharedPreferences.edit().putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, it).apply()
            brightnessStep = it
        }
    }

    private fun adjustColor(color: Long) {
        sharedPreferences.edit().putLong(SharedPreferenceKeys.CURRENT_COLOR, color).apply()
        this.color = com.elfefe.screenbrightness.Color.fromLong(color)
        setOverlayColor()
    }

    private fun setOverlayColor() {
        dimView?.setBackgroundColor(Color.argb(
            brightnessAlpha,
            color.red,
            color.green,
            color.blue
        ))
    }

    private fun toggleOverlay() {
        if (dimView != null)
            hideOverlay()
        else showOverlay()
    }

    private fun showOverlay() {
        createOverlay()
        updateNotification()
    }

    private fun hideOverlay() {
        removeOverlay()
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_lower_brightness_monochrome)
            .setProgress(255, 255 - brightnessAlpha, false)
            .addAction(R.drawable.baseline_arrow_left_24, "Reduce", reduceBrightnessPendingIntent)
            .addAction(R.drawable.baseline_arrow_right_24, "Increase", increaseBrightnessPendingIntent)
            .addAction(
                if (dimView != null) R.drawable.baseline_toggle_off_24
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

    companion object {
        const val CHANNEL_ID = "OverlayServiceChannel"
    }
}
