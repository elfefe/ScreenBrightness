package com.elfefe.screenbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Replace la programmation apres un redemarrage : Android efface toutes les
 * alarmes au reboot.
 *
 * La version precedente reconstituait une heure d'arret en ajoutant **huit
 * heures en dur** a l'heure de debut. Cette heure de fin n'existe nulle part
 * ailleurs : l'ecran de programmation ne propose qu'une heure d'activation.
 * La planification restauree ne correspondait donc a rien de ce que
 * l'utilisateur avait regle. Seule l'activation est reprogrammee.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val preferences = context.getSharedPreferences(
            SharedPreferenceKeys.SCHEDULE_PREFS, Context.MODE_PRIVATE
        )
        if (!preferences.getBoolean(SharedPreferenceKeys.IS_SCHEDULED, false)) return

        val heure = preferences.getInt(SharedPreferenceKeys.HOUR, HEURE_PAR_DEFAUT)
        val minute = preferences.getInt(SharedPreferenceKeys.MINUTE, 0)
        val jours = preferences.getStringSet(SharedPreferenceKeys.DAYS_OF_WEEK, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            .orEmpty()

        if (jours.isEmpty()) return

        AlarmScheduler.scheduleOverlay(
            context = context,
            hour = heure,
            minute = minute,
            daysOfWeek = jours,
            enable = true
        )
    }

    private companion object {
        const val HEURE_PAR_DEFAUT = 22
    }
}
