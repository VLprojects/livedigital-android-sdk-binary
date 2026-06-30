plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "space.livedigital.example.callscore"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
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
    // SDK integration infra (engine DI, PeerAppData, JsonUtils). Exposes the SDK + Koin.
    // `api` so the thin app modules get them transitively for App/MainActivity.
    api(project(":shared"))

    // Compose design system (theme, base components, modifier extensions).
    api(project(":design"))

    // Telephony integration (system dialer + self-managed Core-Telecom flows).
    implementation(libs.androidx.core.telecom)

    // FCM push-initiated calls. The consuming app still applies the google-services plugin
    // and provides its own google-services.json.
    api(platform(libs.firebase.bom))
    api(libs.firebase.messaging)

    // Foreground call service lifecycle.
    implementation(libs.androidx.lifecycle.service)

    // Jetpack Compose UI (CallActivity, MainScreen, CallScreen, components).
    api(libs.androidx.activity.compose)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
}
