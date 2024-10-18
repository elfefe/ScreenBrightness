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
import com.elfefe.screenbrightness.OverlayService.Companion.CHANNEL_ID
import com.elfefe.screenbrightness.ui.theme.LowerBrightnessTheme
import com.elfefe.screenbrightness.views.InformationsPopup
import com.elfefe.screenbrightness.views.MainScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainActivity : ComponentActivity() {
    lateinit var sharedPreferences: SharedPreferences

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
            Toast.makeText(this, "Please enable notification permission", Toast.LENGTH_SHORT).show()
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
        println("New intent received ${intent}")
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askPermissions()

        createNotificationChannel()

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
        /*unregisterReceiver(brightnessReceiver)*/
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
}

@Composable
fun BrightnessScreen(
    initialBrightness: Int, onBrightnessChange: (Int) -> Unit,
    initialBrighnessStep: Int, onBrightnessStepChange: (Int) -> Unit
) {
    var brightness by remember { mutableStateOf(initialBrightness) }
    var brightnessStep by remember { mutableStateOf(initialBrighnessStep) }

    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Screen brightness", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = brightness.toFloat(),
            onValueChange = {
                brightness = it.toInt()
                onBrightnessChange(255 - brightness)
            },
            valueRange = 0f..255f
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Screen brightness step ", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = brightnessStep.toFloat(),
                onValueChange = {
                    brightnessStep = it.toInt()
                    onBrightnessStepChange(brightnessStep)
                },
                valueRange = 1f..85f,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Row(
                modifier = Modifier.width(48.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "$brightnessStep", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
