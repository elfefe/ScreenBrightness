plugins {
    // AGP 9 embarque le support Kotlin : appliquer en plus
    // org.jetbrains.kotlin.android echoue sur un conflit d'extension.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

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
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
