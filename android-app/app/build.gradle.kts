import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Read secrets from local.properties (auto-gitignored, never committed).
// Add these lines to android-app/local.properties:
//   OPENROUTER_API_KEY=sk-or-v1-xxxxxxxx
//   OPENROUTER_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val openRouterKey: String = localProps.getProperty("OPENROUTER_API_KEY", "")
// Default is a FREE model (:free = no usage cost). Override in local.properties.
val openRouterModel: String =
    localProps.getProperty("OPENROUTER_MODEL", "nvidia/nemotron-3-ultra-550b-a55b:free")

android {
    namespace = "com.sambhav.plantdisease"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sambhav.plantdisease"
        minSdk = 24            // Android 7.0+ covers ~98% of devices
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Expose the OpenRouter config to code as BuildConfig fields.
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterKey\"")
        buildConfigField("String", "OPENROUTER_MODEL", "\"$openRouterModel\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true     // needed for the BuildConfig fields above
    }

    // Do NOT compress the .tflite model in the APK; it must be memory-mappable.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- TensorFlow Lite (on-device CNN inference) ---
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // --- Networking for OpenRouter AI recommendations ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
