package com.elfefe.screenbrightness

import android.app.Application

/**
 * Point d'entree du processus.
 *
 * Cette classe installait auparavant un [Thread.UncaughtExceptionHandler] au corps
 * vide. Il avalait tous les plantages : aucune trace, aucune remontee, et le
 * processus n'etait meme pas termine — l'application restait dans un etat
 * indetermine au lieu de se fermer proprement.
 *
 * Il a ete retire. Crashlytics installe son propre gestionnaire au demarrage ;
 * le reinstaller ici l'ecraserait et rendrait la remontee de plantages inerte.
 */
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: com.elfefe.screenbrightness.Application
    }
}
