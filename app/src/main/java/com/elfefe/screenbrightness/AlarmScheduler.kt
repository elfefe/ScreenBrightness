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

        val calendar = prochaineOccurrence(hour, minute, day, Calendar.getInstance())

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
     * Date de la prochaine occurrence d'un horaire hebdomadaire.
     *
     * Extrait de [programmer] pour etre verifiable : c'est la partie de la
     * planification ou une erreur ne se voit qu'une semaine plus tard.
     *
     * @param maintenant instant de reference, injecte plutot que lu de
     *   l'horloge, faute de quoi le comportement dependrait du jour ou le test
     *   s'execute.
     * @return la premiere occurrence **strictement posterieure** a [maintenant].
     */
    fun prochaineOccurrence(
        hour: Int,
        minute: Int,
        day: Int,
        maintenant: Calendar
    ): Calendar {
        val cible = (maintenant.clone() as Calendar).apply {
            // Force la resolution des champs a partir de l'instant : sans cela,
            // le comportement dependrait de la maniere dont l'appelant a
            // construit son Calendar.
            timeInMillis = maintenant.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Le decalage est calcule, plutot que confie a set(DAY_OF_WEEK, ...).
        // Cette methode est silencieusement ignoree quand DAY_OF_MONTH a ete
        // renseigne plus recemment, et son resultat depend par ailleurs du
        // premier jour de la semaine, donc de la locale.
        val ecart = (day - cible.get(Calendar.DAY_OF_WEEK) + JOURS_SEMAINE) % JOURS_SEMAINE
        cible.add(Calendar.DAY_OF_MONTH, ecart)

        // Deja passee : la prochaine occurrence est la semaine suivante. La
        // comparaison est stricte — une alarme fixee a l'instant present serait
        // manquee.
        if (!cible.after(maintenant)) cible.add(Calendar.DAY_OF_MONTH, JOURS_SEMAINE)

        return cible
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

    private const val JOURS_SEMAINE = 7
}
