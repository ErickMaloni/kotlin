// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file (build.gradle.kts)
// Questo file definisce i plugin disponibili per TUTTI i moduli del progetto.

plugins {
    // Plugin per le app Android (obbligatorio)
    id("com.android.application") version "8.13.2" apply false

    // Plugin per il linguaggio Kotlin (obbligatorio)
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false

    // Plugin per i servizi Google (Firebase)
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Plugin per Hilt (Dependency Injection)
    id("com.google.dagger.hilt.android") version "2.51.1" apply false

    // Plugin per Kapt (necessario per Hilt)
    id("org.jetbrains.kotlin.kapt") version "1.9.23" apply false

    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
}

