package com.elfefe.screenbrightness

import android.app.*
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.elfefe.screenbrightness.OverlayService.Companion.CHANNEL_ID
import com.elfefe.screenbrightness.ui.theme.LowerBrightnessTheme
import com.elfefe.screenbrightness.views.InformationsPopup
import com.elfefe.screenbrightness.views.MainScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Main activity of the application.
 * Handles permissions, UI, and communication with the OverlayService.
 */
class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sharedPreferences: SharedPreferences

    var isOverlayEnabled = mutableStateOf(false)
    var brightnessAlpha = mutableIntStateOf(150) // Default brightness
    var brightnessStep = mutableIntStateOf(5) // Default brightness step
    var color = mutableStateOf(com.elfefe.screenbrightness.Color.fromColor(Color.Black)) // Default brightness step

    val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestOverlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    val brightnessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ActionKeys.ADJUST_BRIGHTNESS) {
                brightnessAlpha.intValue = intent.getIntExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha.intValue)
            }
        }
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }

    private lateinit var billingClient: BillingClient

    /**
     * Requests necessary permissions for the app to function correctly.
     * This includes overlay permission, boot completed, foreground service, notifications, and exact alarm scheduling.
     */
    fun askPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                add(android.Manifest.permission.FOREGROUND_SERVICE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                add(android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(android.Manifest.permission.USE_EXACT_ALARM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
        }

        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) requestPermission.launch(permission)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            checkNotificationPermission()

        if (!Settings.canDrawOverlays(this))
            requestOverlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    /**
     * Checks if notification permission is granted on Android Tiramisu (API 33) and above.
     * If not granted or should show rationale, it prompts the user to enable it in settings.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkNotificationPermission() {
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ||
            shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(this,
                resString(R.string.please_enable_notification_permission), Toast.LENGTH_SHORT).show()
            startActivity(Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }
    }

    /**
     * Checks if the app has permission to schedule exact alarms on Android S (API 31) and above.
     * @return True if permission is granted, false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun hasExactAlarmPermission(): Boolean =
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /**
     * Called when the activity is re-launched while at the top of the activity stack.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    /**
     * Called when the activity is first created.
     * Initializes permissions, notification channel, billing client, ads, shared preferences, and UI.
     */
    @OptIn(ExperimentalFoundationApi::class, ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        askPermissions()

        createNotificationChannel()

        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener(purchasesUpdatedListener)
            // Configure other settings.
            .build()

        enableEdgeToEdge()

        MobileAds.initialize(this) {}

        sharedPreferences = getSharedPreferences(SharedPreferenceKeys.APP_PREFS, MODE_PRIVATE)

        isOverlayEnabled.value = sharedPreferences.getBoolean(
            SharedPreferenceKeys.OVERLAY_ENABLED, isOverlayEnabled.value
        )
        brightnessAlpha.intValue = sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS, brightnessAlpha.intValue
        )
        brightnessStep.intValue = sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, brightnessStep.intValue
        )
        color.value = sharedPreferences.getLong(
            SharedPreferenceKeys.CURRENT_COLOR, color.value.toLong()
        ).let { com.elfefe.screenbrightness.Color.fromLong(it) }

        setContent {
            LowerBrightnessTheme {
                MainScreen()
                InformationsPopup()
            }
        }
    }

    /**
     * Called when the activity is becoming visible to the user.
     * Registers the shared preference change listener.
     */
    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS), RECEIVER_EXPORTED)
        else registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS))*/
    }

    /**
     * Called when the activity will start interacting with the user.
     * Starts the OverlayService if permissions are granted.
     */
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startService(Intent(this, OverlayService::class.java))
            return
        }

        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            stopService(Intent(this, OverlayService::class.java))
            if (Settings.canDrawOverlays(this))
                startService(Intent(this, OverlayService::class.java))
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     * Unregisters the shared preference change listener.
     */
    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        /*unregisterReceiver(brightnessReceiver)*/
    }

    /**
     * Called before the activity is destroyed.
     * Stops the OverlayService.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, OverlayService::class.java))
    }

    /**
     * Creates the notification channel for the OverlayService.
     * This is required for Android Oreo (API 26) and above.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Overlay Service Channel", NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Sends an intent to the OverlayService to adjust the brightness.
     * @param brightness The new brightness level (0-255).
     */
    fun adjustBrightness(brightness: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightness)
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    /**
     * Sends an intent to the OverlayService to adjust the brightness step.
     * @param step The new brightness step value.
     */
    fun adjustBrightnessStep(step: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS_STEP
            putExtra(IntentKeys.BRIGHTNESS_STEP, step)
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    /**
     * Sends an intent to the OverlayService to adjust the overlay color.
     * Updates the local color state.
     * @param color The new color for the overlay.
     */
    fun adjustColor(color: com.elfefe.screenbrightness.Color) {
        println("adjustColor: ${this.color.value.hashCode()} to ${color.hashCode()}");
        this.color.value = color
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_COLOR
            putExtra(IntentKeys.UPDATE_COLOR, color.toLong())
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    /**
     * Called when a shared preference is changed.
     * Updates the [isOverlayEnabled] state if the relevant preference changes.
     */
    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        when (key) {
            SharedPreferenceKeys.OVERLAY_ENABLED -> isOverlayEnabled.value = sharedPreferences
                ?.getBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, false) == true
        }
    }

    companion object {
        lateinit var instance: MainActivity

        const val MIN_BRIGHTNESS = 0
        const val MAX_BRIGHTNESS = 255
    }
}
