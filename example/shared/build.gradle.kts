plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "space.livedigital.example.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    val javaVersion = JavaVersion.VERSION_21

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

dependencies {
    api(libs.livedigital.sdk)
    api(libs.koin)
    // Kept for JsonUtils + PeerAppData serialization (used by every sample's connectToChannel).
    // The MoodHood REST/Retrofit stack now lives in :moodhood-api.
    implementation(libs.kotlinx.serialization.json)
}
