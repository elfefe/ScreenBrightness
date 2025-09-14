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
import com.elfefe.screenbrightness.MainActivity.Companion.MAX_BRIGHTNESS
import com.elfefe.screenbrightness.MainActivity.Companion.MIN_BRIGHTNESS

/**
 * Service responsible for displaying and managing the screen dimming overlay.
 * It handles overlay creation, removal, color and brightness adjustments, and notifications.
 */
class OverlayService : Service() {

    lateinit var sharedPreferences: SharedPreferences
    private var dimView: View? = null
    private var windowManager: WindowManager? = null
    private val isOverlayEnabled: Boolean
        get() = sharedPreferences.getBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, false)
    private var brightnessAlpha = 150 // Default brightness (0-255)
    private var brightnessStep = 5 // Default brightness step (0-255)
    private var color = com.elfefe.screenbrightness.Color.fromColor(androidx.compose.ui.graphics.Color.Black) // Default color

    /**
     * Called by the system when the service is first created.
     * Initializes shared preferences and loads initial brightness, step, and color values.
     */
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(SharedPreferenceKeys.APP_PREFS, MODE_PRIVATE)

        brightnessAlpha = sharedPreferences.getInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS, brightnessAlpha)
        brightnessStep = sharedPreferences.getInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, brightnessStep)
        color = com.elfefe.screenbrightness.Color.fromLong(
            sharedPreferences.getLong(SharedPreferenceKeys.CURRENT_COLOR, color.toLong()))
    }

    /**
     * Called by the system every time a client starts the service using [startService].
     * Handles incoming intents, starts the service in the foreground, and creates a notification.
     * @param intent The Intent supplied to [startService], may be null.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntentActions(intent)
        println("onStartCommand: ${intent?.action} - ${intent?.flags}, flags: $flags, startId: $startId, isOverlayEnabled: $isOverlayEnabled")
        startForeground(1, createNotification())
        if (isOverlayEnabled) showOverlay()

        return START_STICKY
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * Removes the overlay if it is currently displayed.
     */
    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy")
        removeOverlay()
    }

    /**
     * Called by the system when a client binds to the service using [bindService].
     * This service does not support binding, so it returns null.
     * @param intent The Intent that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the service. Return null if clients cannot bind to the service.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handles actions received via intents, such as toggling the overlay, adjusting brightness, or changing color.
     * @param intent The intent containing the action to perform.
     */
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

    /**
     * Called by the system when the device configuration changes while your component is running.
     * If the overlay is enabled, it's removed and recreated to adapt to the new configuration.
     * @param newConfig The new device configuration.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        println("onConfigurationChanged: $newConfig $isOverlayEnabled")

        if (!isOverlayEnabled) return

        removeOverlay()
        createOverlay()

        println("onConfigurationChanged after $isOverlayEnabled")
    }

    /**
     * Sends a broadcast intent to the MainActivity to update the brightness UI.
     * Note: This function is currently not used but is kept for potential future use.
     */
    private fun sendBrightnessToActivity() {
        val intent = Intent(IntentKeys.UPDATE_BRIGHTNESS).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha)
        }

        sendBroadcast(intent)
    }

    /**
     * Creates and displays the dimming overlay view on the screen.
     * Initializes the WindowManager and adds a new View with appropriate layout parameters.
     * Sets the overlay color and updates shared preferences.
     */
    private fun createOverlay() {
        println("createOverlay")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val dimViewIsInitialized = dimView != null

        if (!dimViewIsInitialized)
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

        if (!dimViewIsInitialized) {
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
                    windowManager?.maximumWindowMetrics?.bounds?.width()
                        ?: WindowManager.LayoutParams.MATCH_PARENT
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
        }
        println("createOverlay: $windowManager $dimView")
        sharedPreferences.edit { putBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, true) }
    }

    /**
     * Removes the dimming overlay view from the screen.
     * Updates shared preferences to reflect that the overlay is disabled.
     */
    private fun removeOverlay() {
        println("removeOverlay")
        dimView?.let {
            windowManager?.removeView(it)
            dimView = null
        }
    }

    /**
     * Adjusts the brightness of the overlay.
     * Updates shared preferences with the new brightness value and applies it to the overlay.
     * @param alpha The new brightness alpha value (0-255). Values outside this range are coerced.
     */
    private fun adjustBrightness(alpha: Int) {
        alpha.coerceAtLeast(MIN_BRIGHTNESS).coerceAtMost(MAX_BRIGHTNESS).let {
            sharedPreferences.edit { putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS, it) }
            brightnessAlpha = it
            setOverlayColor()
        }
    }

    /**
     * Adjusts the step value used for increasing/decreasing brightness via notification controls.
     * Updates shared preferences with the new step value.
     * @param alpha The new brightness step value (1-85). Values outside this range are coerced.
     */
    private fun adjustBrightnessStep(alpha: Int) {
        alpha.coerceAtLeast(1).coerceAtMost(85).let {
            sharedPreferences.edit { putInt(SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, it) }
            brightnessStep = it
        }
    }

    /**
     * Adjusts the color of the overlay.
     * Updates shared preferences with the new color and applies it to the overlay.
     * @param color The new color represented as a Long (obtained via [com.elfefe.screenbrightness.Color.toLong]).
     */
    private fun adjustColor(color: Long) {
        sharedPreferences.edit { putLong(SharedPreferenceKeys.CURRENT_COLOR, color) }
        this.color = com.elfefe.screenbrightness.Color.fromLong(color)
        setOverlayColor()
    }

    /**
     * Sets the background color of the overlay view based on the current [brightnessAlpha] and [color] values.
     */
    private fun setOverlayColor() {
        dimView?.setBackgroundColor(Color.argb(
            brightnessAlpha,
            color.red,
            color.green,
            color.blue
        ))
    }

    /**
     * Toggles the overlay on or off.
     * If the overlay is currently shown, it hides it. Otherwise, it shows it.
     */
    private fun toggleOverlay() {
        if (dimView != null)
            hideOverlay()
        else showOverlay()
    }

    /**
     * Shows the overlay by creating it and updating the notification.
     */
    private fun showOverlay() {
        println("showOverlay")
        createOverlay()
        updateNotification()
    }

    /**
     * Hides the overlay by removing it and updating the notification.
     */
    private fun hideOverlay() {
        println("hideOverlay")
        removeOverlay()
        sharedPreferences.edit { putBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, false) }
        updateNotification()
    }

    /**
     * Updates the persistent notification for the service.
     * Notifies the NotificationManager to re-display the notification with current state.
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, createNotification())
    }

    /**
     * Creates the persistent notification for the service.
     * The notification includes actions to toggle the overlay, and increase/decrease brightness.
     * @return The constructed [Notification] object.
     */
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
            .setProgress(MAX_BRIGHTNESS, MAX_BRIGHTNESS - brightnessAlpha, false)
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

    /**
     * Determines the appropriate window type for the overlay.
     * @return The WindowManager.LayoutParams window type constant.
     */
    private fun windowType(): Int =
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    /**
     * Gets the appropriate flags for creating PendingIntents.
     * Ensures immutability for security best practices.
     * @return The PendingIntent flags.
     */
    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_IMMUTABLE

    /**
     * Companion object for [OverlayService].
     * Contains constants related to the service.
     */
    companion object {
        /**
         * The ID of the notification channel for the overlay service.
         */
        const val CHANNEL_ID = "OverlayServiceChannel"
    }
}
