package com.elfefe.screenbrightness

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

/**
 * Programmation de l'activation automatique de la surdimpression.
 *
 * Une alarme Android ne se repete pas d'elle-meme quand elle est exacte :
 * [AlarmManager.setExactAndAllowWhileIdle] ne se declenche qu'une fois. La
 * reprogrammation pour la semaine suivante se fait donc dans [AlarmReceiver],
 * au moment ou l'alarme sonne. Sans cela, la planification ne fonctionnait
 * qu'une seule fois par jour selectionne, puis plus jamais.
 */
object AlarmScheduler {

    /**
     * Programme l'activation ou l'extinction de la surdimpression a une heure
     * donnee, pour chacun des jours indiques.
     *
     * @param daysOfWeek jours au sens des constantes [Calendar] (par exemple
     *   [Calendar.MONDAY]).
     * @param enable `true` pour programmer l'activation, `false` pour l'arret.
     */
    fun scheduleOverlay(
        context: Context,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        enable: Boolean
    ) {
        for (day in daysOfWeek) {
            programmer(context, hour, minute, day, enable)
        }
    }

    /**
     * Programme une seule occurrence, pour un seul jour de la semaine.
     *
     * Utilise aussi par [AlarmReceiver] pour reconduire l'alarme d'une semaine
     * une fois qu'elle a sonne.
     */
    fun programmer(
        context: Context,
        hour: Int,
        minute: Int,
        day: Int,
        enable: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, day)

            // L'heure est deja passee aujourd'hui : la prochaine occurrence est
            // la semaine suivante.
            if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
        }

        val pendingIntent = intentPour(context, day, enable)

        // setExact ne se declenche pas en mode Doze, c'est-a-dire justement la
        // nuit, quand cette application sert.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Annule une programmation existante.
     *
     * @param enable doit valoir ce qui avait ete passe a [scheduleOverlay] :
     *   activation et arret sont deux alarmes distinctes, identifiees par des
     *   codes differents.
     */
    fun cancelScheduledOverlay(
        context: Context,
        daysOfWeek: Set<Int>,
        enable: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in daysOfWeek) {
            alarmManager.cancel(intentPour(context, day, enable))
        }
    }

    /**
     * L'intention en attente identifiant une alarme.
     *
     * Le couple (jour, activation ou arret) doit produire exactement le meme
     * `requestCode` a la programmation et a l'annulation, sans quoi
     * [AlarmManager.cancel] ne trouve rien a annuler.
     */
    private fun intentPour(context: Context, day: Int, enable: Boolean): PendingIntent {
        val action =
            if (enable) ActionKeys.ACTION_START_OVERLAY else ActionKeys.ACTION_STOP_OVERLAY
        val requestCode = if (enable) day else day + DECALAGE_ARRET

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(IntentKeys.JOUR_PROGRAMME, day)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Separe les codes d'arret de ceux d'activation. */
    private const val DECALAGE_ARRET = 1000
}
