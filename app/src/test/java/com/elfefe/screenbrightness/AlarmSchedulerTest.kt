package com.elfefe.screenbrightness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Le calcul de la prochaine occurrence est l'endroit ou une erreur ne se voit
 * qu'une semaine plus tard. C'est aussi celui qui a laisse passer une
 * planification ne se declenchant qu'une seule fois.
 */
class AlarmSchedulerTest {

    /** Instant de reference : mercredi 15 juillet 2026, 12 h 00, UTC. */
    private fun mercrediMidi(): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.JULY, 15, 12, 0, 0)
        }

    private fun occurrence(heure: Int, minute: Int, jour: Int): Calendar =
        AlarmScheduler.prochaineOccurrence(heure, minute, jour, mercrediMidi())

    @Test
    fun `une heure encore a venir aujourd'hui tombe aujourd'hui`() {
        val resultat = occurrence(22, 0, Calendar.WEDNESDAY)

        assertEquals(15, resultat.get(Calendar.DAY_OF_MONTH))
        assertEquals(22, resultat.get(Calendar.HOUR_OF_DAY))
        assertTrue(resultat.after(mercrediMidi()))
    }

    @Test
    fun `une heure deja passee aujourd'hui bascule a la semaine suivante`() {
        val resultat = occurrence(8, 0, Calendar.WEDNESDAY)

        // Le 22, pas le 16 : c'est le meme jour de semaine qui compte.
        assertEquals(22, resultat.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, resultat.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `l'heure pile ne compte pas comme a venir`() {
        // Une alarme fixee a l'instant present serait manquee : elle doit
        // basculer a la semaine suivante plutot que d'etre programmee dans le
        // passe immediat.
        val resultat = occurrence(12, 0, Calendar.WEDNESDAY)

        assertEquals(22, resultat.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `un jour plus tard dans la semaine tombe cette semaine`() {
        val resultat = occurrence(22, 0, Calendar.FRIDAY)

        assertEquals(17, resultat.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FRIDAY, resultat.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `un jour deja passe dans la semaine tombe la semaine suivante`() {
        val resultat = occurrence(22, 0, Calendar.MONDAY)

        assertEquals(20, resultat.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MONDAY, resultat.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `les secondes sont remises a zero`() {
        val avecSecondes = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.JULY, 15, 12, 0, 37)
            set(Calendar.MILLISECOND, 500)
        }

        val resultat =
            AlarmScheduler.prochaineOccurrence(22, 0, Calendar.WEDNESDAY, avecSecondes)

        assertEquals(0, resultat.get(Calendar.SECOND))
        assertEquals(0, resultat.get(Calendar.MILLISECOND))
    }

    @Test
    fun `chaque jour de la semaine donne une occurrence future et unique`() {
        val jours = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        val dates = jours.map { jour ->
            val resultat = occurrence(22, 0, jour)
            assertTrue("$jour doit etre dans le futur", resultat.after(mercrediMidi()))
            assertEquals(jour, resultat.get(Calendar.DAY_OF_WEEK))
            resultat.timeInMillis
        }

        assertEquals("sept jours distincts", 7, dates.toSet().size)
    }
}
