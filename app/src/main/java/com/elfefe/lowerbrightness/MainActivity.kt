package com.elfefe.lowerbrightness

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elfefe.lowerbrightness.ui.theme.LowerBrightnessTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    lateinit var sharedPreferences: SharedPreferences

    private var brightnessAlpha = mutableStateOf(150) // Default brightness
    private var brightnessStep = mutableStateOf(51) // Default brightness step

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
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            )
                requestPermission.launch(permission)
        }

        if (!Settings.canDrawOverlays(this))
            requestOverlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasExactAlarmPermission()) {
//            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
//            startActivity(intent)
//        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasExactAlarmPermission(): Boolean =
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("New intent received ${intent}")
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askPermissions()

        if (Settings.canDrawOverlays(this))
            startOverlayService()

        enableEdgeToEdge()

        sharedPreferences = getSharedPreferences(SharedPreferenceKeys.APP_PREFS, MODE_PRIVATE)

        brightnessAlpha.value = 255 - sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS,
            intent.getIntExtra(IntentKeys.BRIGHTNESS_LEVEL, brightnessAlpha.value)
        )
        brightnessStep.value = sharedPreferences.getInt(
            SharedPreferenceKeys.CURRENT_BRIGHTNESS_STEP,
            intent.getIntExtra(IntentKeys.BRIGHTNESS_STEP, brightnessStep.value)
        )

        setContent {
            LowerBrightnessTheme {
                val scope = rememberCoroutineScope()

                val pages by remember { mutableStateOf(listOf(
                    "brightness" to R.drawable.baseline_settings_brightness_24,
                    "alarm" to R.drawable.baseline_alarm_24
                )) }

                val pageScrollState = rememberScrollState()
                val pagerState = rememberPagerState(pageCount = { pages.size })

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    startForegroundService(
                                        Intent(this, OverlayService::class.java).apply {
                                            action = ActionKeys.TOGGLE_OVERLAY
                                        }
                                    )
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.mipmap.ic_lower_brightness_monochrome),
                                    contentDescription = "Toggle overlay"
                                )
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End,
                        bottomBar = {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                BottomAppBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(pages) { page ->
                                            IconButton(
                                                onClick = {
                                                    scope.launch(Dispatchers.Default) {
                                                        pagerState.animateScrollToPage(
                                                            pages.indexOf(page)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(page.second),
                                                    contentDescription = page.first,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            Column(
                                modifier = Modifier
                                    .padding(top = 32.dp, bottom = it.calculateBottomPadding())
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.Start
                            ) {
                                when (page) {
                                    0 -> BrightnessScreen(
                                        initialBrightness = brightnessAlpha.value,
                                        onBrightnessChange = { newBrightness ->
                                            adjustBrightness(newBrightness)
                                        },
                                        initialBrighnessStep = brightnessStep.value,
                                        onBrightnessStepChange = { step ->
                                            adjustBrightnessStep(step)
                                        }
                                    )

                                    1 -> ScheduleScreen(this@MainActivity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS), RECEIVER_EXPORTED)
        else registerReceiver(brightnessReceiver, IntentFilter(IntentKeys.UPDATE_BRIGHTNESS))*/
    }

    override fun onStop() {
        super.onStop()
        /*unregisterReceiver(brightnessReceiver)*/
    }

    private fun adjustBrightness(brightness: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS
            putExtra(IntentKeys.BRIGHTNESS_LEVEL, brightness)
        }
        ContextCompat.startForegroundService(this, adjustIntent)
    }

    private fun adjustBrightnessStep(step: Int) {
        val adjustIntent = Intent(this, OverlayService::class.java).apply {
            action = ActionKeys.ADJUST_BRIGHTNESS_STEP
            putExtra(IntentKeys.BRIGHTNESS_STEP, step)
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
        modifier = Modifier.padding(16.dp),
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