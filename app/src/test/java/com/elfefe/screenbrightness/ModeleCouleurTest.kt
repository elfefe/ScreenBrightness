package com.elfefe.screenbrightness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce que l'utilisateur voit reellement a l'ecran : la couleur du voile est le
 * produit de la saturation choisie et de la luminance, converti en composantes
 * 0-255 avant d'etre passe a `Color.argb`.
 */
class ModeleCouleurTest {

    @Test
    fun `a pleine luminance les composantes valent la saturation`() {
        val couleur = Color(1f, 1f, Color.Saturation(1f, 0.5f, 0f))

        assertEquals(255, couleur.red)
        assertEquals(128, couleur.green)
        assertEquals(0, couleur.blue)
    }

    @Test
    fun `une luminance nulle eteint toutes les composantes`() {
        val couleur = Color(0f, 1f, Color.Saturation(1f, 1f, 1f))

        assertEquals(0, couleur.red)
        assertEquals(0, couleur.green)
        assertEquals(0, couleur.blue)
    }

    @Test
    fun `la luminance attenue proportionnellement`() {
        val pleine = Color(1f, 1f, Color.Saturation(1f, 1f, 1f))
        val demie = Color(0.5f, 1f, Color.Saturation(1f, 1f, 1f))

        assertTrue("la moitie doit etre plus sombre", demie.red < pleine.red)
        // 0,5 quantifie donne 128/255 : la composante suit exactement.
        assertEquals(128, demie.red)
    }

    @Test
    fun `les valeurs hors bornes sont ramenees dans l'intervalle`() {
        val trop = Color(5f, 5f, Color.Saturation(3f, 3f, 3f))

        assertEquals(1f, trop.luminance)
        assertEquals(1f, trop.alpha)
        assertEquals(1f, trop.saturation.red)
    }

    @Test
    fun `une luminance negative est prise en valeur absolue`() {
        val negative = Color(-0.5f, 1f, Color.Saturation(-1f, 1f, 1f))

        assertEquals(0.5f, negative.luminance, 1f / 255)
        assertEquals(1f, negative.saturation.red)
    }

    @Test
    fun `deux couleurs identiques sont egales et partagent leur empreinte`() {
        val a = Color(0.3f, 0.7f, Color.Saturation(0.2f, 0.4f, 0.6f))
        val b = Color(0.3f, 0.7f, Color.Saturation(0.2f, 0.4f, 0.6f))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `une couleur differente ne se confond pas avec une autre`() {
        val a = Color(0.3f, 0.7f, Color.Saturation(0.2f, 0.4f, 0.6f))
        val autreLuminance = Color(0.4f, 0.7f, Color.Saturation(0.2f, 0.4f, 0.6f))
        val autreSaturation = Color(0.3f, 0.7f, Color.Saturation(0.9f, 0.4f, 0.6f))

        assertTrue(a != autreLuminance)
        assertTrue(a != autreSaturation)
    }

    @Test
    fun `le pourcentage affiche couvre bien les bornes`() {
        assertEquals(0, 0.colorToPercent())
        assertEquals(100, 255.colorToPercent())
        assertEquals(50, 128.colorToPercent())
    }
}
