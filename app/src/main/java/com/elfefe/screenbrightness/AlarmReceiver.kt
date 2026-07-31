package com.elfefe.screenbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Recoit les alarmes de programmation et agit sur [OverlayService].
 *
 * Deux corrections notables par rapport a la version precedente :
 *
 * - l'alarme est **reconduite d'une semaine** des qu'elle sonne. Une alarme
 *   exacte ne se repete pas, et rien ne la reprogrammait : la planification ne
 *   se declenchait donc qu'une seule fois, puis plus jamais ;
 * - l'arret passe desormais par une action envoyee au service, la ou
 *   `stopService` le detruisait — emportant avec lui la notification de
 *   controle, donc toute possibilite de reprendre la main.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            this.action = action
        }

        when (action) {
            ActionKeys.ACTION_START_OVERLAY,
            ActionKeys.ACTION_STOP_OVERLAY -> {
                ContextCompat.startForegroundService(context, serviceIntent)
                reconduire(context, intent, action == ActionKeys.ACTION_START_OVERLAY)
            }

            else -> Log.w(TAG, "Action inattendue : $action")
        }
    }

    /**
     * Reprogramme la meme alarme pour la semaine suivante.
     *
     * L'heure est relue des preferences plutot que transportee dans l'intention :
     * si l'utilisateur a change son horaire entre-temps, c'est le nouveau qui
     * doit s'appliquer.
     */
    private fun reconduire(context: Context, intent: Intent, activation: Boolean) {
        val jour = intent.getIntExtra(IntentKeys.JOUR_PROGRAMME, -1)
        if (jour == -1) return

        val preferences = context.getSharedPreferences(
            SharedPreferenceKeys.SCHEDULE_PREFS, Context.MODE_PRIVATE
        )
        if (!preferences.getBoolean(SharedPreferenceKeys.IS_SCHEDULED, false)) return

        val heure = preferences.getInt(SharedPreferenceKeys.HOUR, HEURE_PAR_DEFAUT)
        val minute = preferences.getInt(SharedPreferenceKeys.MINUTE, 0)

        AlarmScheduler.programmer(context, heure, minute, jour, activation)
    }

    private companion object {
        const val TAG = "AlarmReceiver"
        const val HEURE_PAR_DEFAUT = 22
    }
}
