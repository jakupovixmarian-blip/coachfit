import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

// Keystore di debug FISSO, versionato nel repository: senza di esso ogni macchina
// genera un debug.keystore diverso e Android rifiuta l'aggiornamento dell'app
// (INSTALL_FAILED_UPDATE_INCOMPATIBLE) perche la firma cambia. Con una firma
// stabile le build si aggiornano normalmente sul dispositivo.
val debugKeystoreFile = rootProject.file("fitcoach-debug.keystore")
val hasStableDebugKeystore = debugKeystoreFile.exists()

android {
    namespace = "com.fitcoach.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fitcoach.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 35
        versionName = "2.5.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        if (hasStableDebugKeystore) {
            create("debugStable") {
                storeFile = debugKeystoreFile
                storePassword = "fitcoach"
                keyAlias = "fitcoach-debug"
                keyPassword = "fitcoach"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (hasStableDebugKeystore) {
                signingConfig = signingConfigs.getByName("debugStable")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        // I test che chiamano un provider AI reale leggono la chiave
        // dall'ambiente. Senza questo passaggio la variabile non raggiunge la
        // JVM di test e il test si salta silenziosamente: sembrerebbe verde
        // senza aver mai contattato il modello.
        unitTests.all { test ->
            // Dichiarati come INPUT del task, non solo letti: Gradle non traccia
            // le variabili d'ambiente, quindi senza questo cambiare REAL_AI_MODEL
            // riuserebbe l'esito del modello precedente.
            //
            // Della chiave si dichiara l'HASH, mai il valore: `inputs.property`
            // finisce nei log e nei report di build, e ci scriverebbe la chiave
            // in chiaro. L'hash cambia quando cambia la chiave, che e' tutto
            // cio' che serve alla cache.
            listOf("REAL_AI_MODEL", "REAL_AI_SCOPE").forEach { name ->
                val value = System.getenv(name) ?: ""
                test.systemProperty(name, value)
                test.inputs.property(name, value)
            }
            // La chiave NON si passa come system property: finirebbe nella riga
            // di comando del processo di test, che Gradle scrive nei log con
            // --info. La JVM di test eredita gia' l'ambiente del daemon, quindi
            // basta dichiararne l'hash come input per invalidare la cache.
            val apiKey = System.getenv("OPENROUTER_API_KEY") ?: ""
            test.inputs.property(
                "OPENROUTER_API_KEY_hash",
                apiKey.hashCode().toString()
            )
        }
    }

    sourceSets {
        // Gli schemi Room esportati servono a MigrationTestHelper (test unitari
        // Robolectric, che leggono gli asset della variante debug). Il percorso
        // "schemas/<db>/<version>.json" combacia con quello atteso dall'helper.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.health.connect)
    implementation(libs.guava)

    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.security.crypto)

    // Fotocamera e lettura codice a barre (stesso approccio di Yazio).
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
