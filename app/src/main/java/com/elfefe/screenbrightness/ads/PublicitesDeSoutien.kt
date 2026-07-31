package com.elfefe.screenbrightness.ads

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.elfefe.screenbrightness.BuildConfig

/**
 * Publicite de soutien, entierement volontaire.
 *
 * L'utilisateur declenche l'annonce depuis le menu ; rien ne s'affiche de
 * lui-meme. C'est ce que les regles AdMob exigent d'une annonce avec
 * recompense, et c'est aussi ce qu'annoncent les textes de l'application.
 *
 * La version precedente — le composable `AdsScreen`, supprime — faisait
 * l'inverse : elle affichait l'annonce des qu'elle etait chargee, puis
 * remettait la reference a null a la fermeture, ce qui relancait le chargement
 * et donc l'affichage. En boucle, sans consentement, et sans que l'utilisateur
 * l'ait demande. Elle n'etait heureusement appelee nulle part.
 *
 * Le SDK n'est initialise qu'une fois le consentement obtenu : demarrer le SDK
 * avant est ce qui vaut les mises en demeure au titre du RGPD.
 */
class PublicitesDeSoutien(private val activite: Activity) {

    /** Ce que l'interface a besoin de savoir, et rien de plus. */
    enum class Etat {
        /** Consentement pas encore obtenu, ou refuse. */
        INDISPONIBLE,

        /** Une annonce est en cours de chargement. */
        CHARGEMENT,

        /** Une annonce est prete a etre proposee. */
        PRETE,
    }

    var etat by mutableStateOf(Etat.INDISPONIBLE)
        private set

    private var annonce: RewardedAd? = null
    private var sdkDemarre = false

    private val consentement: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activite)

    /**
     * A appeler une fois au demarrage de l'activite.
     *
     * Recueille le consentement si la reglementation de la zone l'impose, puis
     * initialise le SDK et precharge une annonce. Un echec n'est jamais fatal :
     * l'application fonctionne entierement sans publicite.
     */
    fun demarrer() {
        consentement.requestConsentInfoUpdate(
            activite,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activite) { erreur ->
                    if (erreur != null) {
                        Log.w(TAG, "Formulaire de consentement : ${erreur.message}")
                    }
                    demarrerSdkSiPossible()
                }
            },
            { erreur ->
                // Reseau absent, par exemple. On ne diffuse rien, et c'est tout.
                Log.w(TAG, "Consentement indisponible : ${erreur.message}")
            }
        )
    }

    private fun demarrerSdkSiPossible() {
        if (!consentement.canRequestAds()) {
            etat = Etat.INDISPONIBLE
            return
        }
        if (!sdkDemarre) {
            sdkDemarre = true
            MobileAds.initialize(activite) { charger() }
        } else {
            charger()
        }
    }

    private fun charger() {
        if (annonce != null || etat == Etat.CHARGEMENT) return
        etat = Etat.CHARGEMENT

        RewardedAd.load(
            activite,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(pub: RewardedAd) {
                    annonce = pub
                    etat = Etat.PRETE
                }

                override fun onAdFailedToLoad(erreur: LoadAdError) {
                    Log.w(TAG, "Chargement echoue : ${erreur.message}")
                    annonce = null
                    etat = Etat.INDISPONIBLE
                }
            }
        )
    }

    /**
     * Affiche l'annonce, si elle est prete.
     *
     * @param surRecompense appele uniquement si l'utilisateur a regarde
     *   l'annonce jusqu'au bout.
     * @return `false` si rien n'etait pret — l'appelant peut alors le dire a
     *   l'utilisateur plutot que de laisser le bouton sans effet.
     */
    fun proposer(surRecompense: () -> Unit): Boolean {
        val pub = annonce ?: run {
            // Rien de pret : on en redemande une pour la prochaine fois.
            demarrerSdkSiPossible()
            return false
        }

        pub.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                annonce = null
                etat = Etat.INDISPONIBLE
                // Rechargee pour la fois suivante, mais jamais reaffichee seule.
                charger()
            }

            override fun onAdFailedToShowFullScreenContent(erreur: com.google.android.gms.ads.AdError) {
                Log.w(TAG, "Affichage echoue : ${erreur.message}")
                annonce = null
                etat = Etat.INDISPONIBLE
                charger()
            }
        }

        annonce = null
        etat = Etat.INDISPONIBLE
        pub.show(activite) { surRecompense() }
        return true
    }

    private companion object {
        const val TAG = "PublicitesDeSoutien"
    }
}
