package com.elfefe.screenbrightness

import android.app.*
import android.content.BroadcastReceiver
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

class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sharedPreferences: SharedPreferences

    var isOverlayEnabled = mutableStateOf(false)
    var brightnessAlpha = mutableStateOf(150) // Default brightness
    var brightnessStep = mutableStateOf(51) // Default brightness step
    var color = mutableStateOf(com.elfefe.screenbrightness.Color.fromColor(Color.Black)) // Default brightness step

    val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestOverlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    val brightnessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ActionKeys.ADJUST_BRIGHTNESS) {
                brightnessAlpha.value = intent.getIntExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha.value)
            }
        }
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }

    private lateinit var billingClient: BillingClient

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

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasExactAlarmPermission(): Boolean =
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        brightnessAlpha.value = 255 - sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS, brightnessStep.value
        )
        brightnessStep.value = sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP, brightnessStep.value
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

    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS), RECEIVER_EXPORTED)
        else registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS))*/
    }

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

    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        /*unregisterReceiver(brightnessReceiver)*/
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Overlay Service Channel", NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun adjustBrightness(brightness: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightness)
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    fun adjustBrightnessStep(step: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS_STEP
            putExtra(IntentKeys.BRIGHTNESS_STEP, step)
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    fun adjustColor(color: com.elfefe.screenbrightness.Color) {
        this.color.value = color
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_COLOR
            putExtra(IntentKeys.UPDATE_COLOR, color.toLong())
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        when (key) {
            SharedPreferenceKeys.OVERLAY_ENABLED -> isOverlayEnabled.value = sharedPreferences
                ?.getBoolean(SharedPreferenceKeys.OVERLAY_ENABLED, false) == true
        }
    }
}
