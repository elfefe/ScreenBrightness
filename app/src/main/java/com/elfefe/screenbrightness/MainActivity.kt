package com.elfefe.screenbrightness

import android.app.*
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
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
import com.elfefe.screenbrightness.ads.PublicitesDeSoutien

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

    /** Publicite de soutien, declenchee par l'utilisateur depuis le menu. */
    lateinit var publicites: PublicitesDeSoutien
        private set

    /**
     * Requests necessary permissions for the app to function correctly.
     * This includes overlay permission, boot completed, foreground service, notifications, and exact alarm scheduling.
     */
    fun askPermissions() {
        // Une seule permission d'exécution est réellement demandable ici.
        // L'ancienne version en passait six a `requestPermission.launch` dans
        // une boucle : SYSTEM_ALERT_WINDOW, RECEIVE_BOOT_COMPLETED,
        // FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE et USE_EXACT_ALARM
        // ne sont pas des permissions d'execution — les demander ainsi n'a
        // aucun effet. Et Android ne traite qu'une demande a la fois : les
        // lancements successifs s'ecrasaient les uns les autres.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // La surdimpression est la fonction meme de l'application : sans cette
        // autorisation, rien ne s'affiche. Elle ne se donne que depuis les
        // reglages du systeme.
        if (!Settings.canDrawOverlays(this))
            requestOverlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }

    /**
     * Ouvre les reglages d'alarmes exactes, si l'utilisateur en a besoin.
     *
     * Appelee uniquement au moment de programmer un horaire. L'ancienne version
     * lancait cet ecran depuis `onCreate` : l'utilisateur etait expedie dans les
     * reglages du systeme des l'ouverture de l'application, sans avoir rien
     * demande, et a chaque lancement.
     */
    fun demanderAlarmesExactes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasExactAlarmPermission()) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    /**
     * Checks if the app has permission to schedule exact alarms on Android S (API 31) and above.
     * @return True if permission is granted, false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun hasExactAlarmPermission(): Boolean =
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

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

        // Le SDK publicitaire n'est plus demarre ici : PublicitesDeSoutien
        // attend d'abord le consentement, puis initialise MobileAds lui-meme.
        publicites = PublicitesDeSoutien(this)
        publicites.demarrer()

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
        // Se lie au service pour pouvoir lui pousser les changements de couleur
        // et de luminosite directement, sans un startForegroundService par
        // evenement de glissement.
        bindService(
            Intent(this, OverlayService::class.java),
            overlayConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Called when the activity will start interacting with the user.
     * Starts the OverlayService if permissions are granted.
     */
    /**
     * S'assure que le service tourne, sans le redemarrer.
     *
     * La version precedente appelait `stopService` puis `startService` a chaque
     * retour au premier plan : la surdimpression disparaissait donc a chaque
     * fois que l'utilisateur rouvrait l'application, avant d'etre recreee.
     *
     * `onDestroy` arretait par ailleurs le service, ce qui allait contre la
     * fonction meme de l'application — le filtre s'eteignait des que l'ecran
     * etait ferme. Cette surcharge a ete retiree : le service s'arrete quand
     * l'utilisateur le decide, depuis la notification ou l'interrupteur.
     */
    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) return
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
    }

    /**
     * Called when the activity is no longer visible to the user.
     * Unregisters the shared preference change listener.
     */
    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (overlayLie) {
            unbindService(overlayConnection)
            overlayLie = false
            overlayService = null
        }
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

    /** Service lie, quand il l'est : voir [overlayConnection]. */
    private var overlayService: OverlayService? = null
    private var overlayLie = false

    private val overlayConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            overlayService = (service as? OverlayService.LocalBinder)?.service
            overlayLie = overlayService != null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            overlayService = null
            overlayLie = false
        }
    }

    /**
     * Ajuste la luminosite du filtre.
     *
     * Passe par le service lie quand il l'est — un simple appel de methode,
     * pendant un glissement — et retombe sur un `startForegroundService`
     * sinon, par exemple avant que la liaison ne soit etablie.
     *
     * @param brightness niveau 0-255.
     */
    fun adjustBrightness(brightness: Int) {
        overlayService?.let {
            it.mettreAJourLuminositeEnDirect(brightness)
            return
        }
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
        this.color.value = color
        overlayService?.let {
            it.mettreAJourCouleurEnDirect(color.toLong())
            return
        }
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
