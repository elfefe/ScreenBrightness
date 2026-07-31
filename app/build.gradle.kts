import java.util.Properties

plugins {
    // AGP 9 embarque le support Kotlin : appliquer en plus
    // org.jetbrains.kotlin.android echoue sur un conflit d'extension.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Identifiants AdMob reels, lus depuis local.properties qui n'est pas suivi par
// git. En leur absence — un clone neuf, une machine de CI — on retombe sur les
// identifiants de demonstration publics de Google. Le projet compile donc
// toujours, et personne ne sert d'annonces reelles par accident.
val proprietesLocales = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

/** Identifiants de test publies par Google, volontairement en clair. */
val admobAppIdTest = "ca-app-pub-3940256099942544~3347511713"
val admobRecompenseIdTest = "ca-app-pub-3940256099942544/5224354917"

val admobAppId: String = proprietesLocales.getProperty("admob.appId") ?: admobAppIdTest
val admobRecompenseId: String =
    proprietesLocales.getProperty("admob.rewardedId") ?: admobRecompenseIdTest

// Signature release. Le keystore n'est pas dans le depot (`.gitignore`, *.jks)
// et son mot de passe vient de local.properties ou de l'environnement, pour la
// CI. Les deux sont sauvegardes dans Vault sous screenbrightness_keystore_*.
// Perdre cette cle rendrait toute mise a jour de l'application impossible.
val fichierKeystore = rootProject.file("keystore/release.jks")
val motDePasseKeystore: String? =
    proprietesLocales.getProperty("keystore.password") ?: System.getenv("KEYSTORE_PASSWORD")
val signatureDisponible = fichierKeystore.exists() && !motDePasseKeystore.isNullOrBlank()

android {
    namespace = "com.elfefe.screenbrightness"
    // Compile contre 37, mais reste cible sur 36 : monter targetSdk change le
    // comportement du systeme a l'execution, ce qui n'est pas du ressort d'une
    // remise en etat de la chaine de build.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.elfefe.screenbrightness"
        minSdk = 26
        targetSdk = 36
        // Play : code 2 en prod (1.1). Codes 3 et 4 : builds de test trickstore
        // (icone). Le depot Play refuse tout code deja utilise, d'ou le 5.
        versionCode = 5
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Declaree seulement si la cle est la : un clone sans keystore doit
    // continuer a construire le debug sans que la configuration echoue.
    signingConfigs {
        if (signatureDisponible) {
            create("release") {
                storeFile = fichierKeystore
                storePassword = motDePasseKeystore
                keyAlias = "screenbrightness"
                keyPassword = motDePasseKeystore
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRecompenseId\"")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Le debug ne sert jamais d'annonces reelles : cliquer sur ses
            // propres annonces en developpement fait suspendre un compte AdMob.
            manifestPlaceholders["admobAppId"] = admobAppIdTest
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRecompenseIdTest\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    constraints {
        // Le projet n'utilise pas Fragment, mais play-services-ads le fait
        // remonter en 1.1.0. registerForActivityResult exige au moins 1.3.0 —
        // lint le refuse en erreur. Une contrainte suffit : inutile d'ajouter
        // une dependance directe sur une API dont on ne se sert pas.
        implementation("androidx.fragment:fragment:1.8.9")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.google.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.ads)
    // Recueil du consentement : exige par Google pour diffuser dans l'EEE.
    implementation(libs.user.messaging.platform)
    // Construit dans MainActivity mais jamais connecte : la decision de finir
    // l'abonnement ou de retirer ce code appartient a GEN-29. La version reste
    // en 7.1.1 tant que ce choix n'est pas fait — billing 8+ change la signature
    // de enablePendingPurchases().
    implementation(libs.billing.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
