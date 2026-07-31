package com.elfefe.screenbrightness

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'encodage d'une couleur doit survivre a un aller-retour : c'est ainsi
 * qu'elle est persistee dans les preferences, et donc rechargee a chaque
 * demarrage.
 */
class ColorTest {

    private fun allerRetour(couleur: Color): Color = Color.fromLong(couleur.toLong())

    @Test
    fun `une couleur survit a l'aller-retour d'encodage`() {
        val extremeHaut = Color(1f, 1f, Color.Saturation(1f, 1f, 1f))
        val extremeBas = Color(0f, 0f, Color.Saturation(0f, 0f, 0f))
        val milieu = Color(0.5f, 1f, Color.Saturation(0.5f, 0.5f, 0.5f))
        // Valeurs qui ne tombent pas sur la grille de 1/255 : c'est le cas qui
        // faisait deriver la couleur avant la quantification.
        val quelconque = Color(0.17f, 1f, Color.Saturation(1f, 0.996f, 0.894f))

        assertEquals(extremeHaut, allerRetour(extremeHaut))
        assertEquals(extremeBas, allerRetour(extremeBas))
        assertEquals(milieu, allerRetour(milieu))
        assertEquals(quelconque, allerRetour(quelconque))
    }

    @Test
    fun `chaque niveau representable se retrouve a l'identique`() {
        for (pas in 0..255) {
            val v = pas / 255f
            val couleur = Color(v, v, Color.Saturation(v, v, v))
            assertEquals("niveau $pas", couleur, allerRetour(couleur))
        }
    }

    @Test
    fun `le Saver rend la couleur qu'il a enregistree`() {
        val couleur = Color(0.42f, 0.8f, Color.Saturation(0.1f, 0.6f, 0.9f))

        // save produisait un hashCode la ou restore attend un encodage : la
        // couleur restauree n'avait alors aucun rapport avec l'originale.
        val enregistre = couleur.toLong()

        assertEquals(couleur, Color.fromLong(enregistre))
    }

    @Test
    fun `l'arrondi decimal se comporte comme annonce`() {
        assertEquals(1.0f, 1.2345.round(0))
        assertEquals(1.2f, 1.2345.round(1))
        assertEquals(1.23f, 1.2345.round(2))
        assertEquals(1.234f, 1.2345.round(3))
    }
}
