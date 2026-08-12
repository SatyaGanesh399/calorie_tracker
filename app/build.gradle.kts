import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------
// Optional API keys are read from local.properties, which is git-ignored.
// The app works fully without any of them (Open Food Facts needs no key).
// See README "Security" for the limitations of client-side keys.
// ---------------------------------------------------------------------------
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val usdaApiKey: String = (localProps.getProperty("USDA_API_KEY") ?: "").trim()

android {
    namespace = "com.satya.calorietracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.satya.calorietracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "USDA_API_KEY", "\"$usdaApiKey\"")
        buildConfigField("String", "OFF_USER_AGENT", "\"CalorieTracker/1.0 (personal-use Android app)\"")
    }

    // ---------------------------------------------------------------------
    // Release signing.
    //
    // Preference order:
    //   1. A real keystore at ~/release.jks with credentials in env vars.
    //      CI restores this from the KEYSTORE_BASE64 repository secret.
    //   2. The Android debug keystore, so a local `assembleRelease` still
    //      produces something installable without any setup.
    //
    // Switching from (2) to (1) later means uninstalling the app first, because
    // Android refuses to update an APK signed with a different key.
    // ---------------------------------------------------------------------
    val userHome: String = System.getProperty("user.home")
    val releaseKeystore = file("$userHome/release.jks")
    val debugKeystore = file("$userHome/.android/debug.keystore")

    val hasOwnKeystore =
        releaseKeystore.exists() && !System.getenv("KEYSTORE_PASSWORD").isNullOrBlank()

    signingConfigs {
        create("personal") {
            if (hasOwnKeystore) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "calorietracker"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            } else {
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }

        // CI runs on a fresh machine every time, and AGP generates a brand new random
        // debug keystore when it doesn't find one. That gives every build a different
        // signature, which Android treats as a different app — so installing a new
        // build would force an uninstall and wipe the diary.
        //
        // Pointing debug at the same stable keystore fixes that: every APK signs
        // identically, so new builds install straight over the old one.
        getByName("debug") {
            if (hasOwnKeystore) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "calorietracker"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            }
            // Otherwise leave AGP's default alone — fine for local development,
            // where the keystore persists between builds anyway.
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore.exists() || debugKeystore.exists()) {
                signingConfig = signingConfigs.getByName("personal")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)

    // Background work + widgets
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Camera + barcode
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
