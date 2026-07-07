plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("kotlin-parcelize")
}

android {
    namespace = "space.livedigital.example"
    compileSdk = 36

    defaultConfig {
        // Kept as the original id so the existing Firebase project's google-services.json
        // (registered for package "space.livedigital.example") keeps working for push.
        applicationId = "space.livedigital.example"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    // SDK integration infra (engine DI, PeerAppData, JsonUtils) + Koin.
    implementation(project(":shared"))

    // Compose design system (theme, base components, modifier extensions).
    implementation(project(":design"))

    // Code shared verbatim with :samples:calls-push — permissions dashboard, telecom endpoint
    // model, CallType, shared Compose components.
    implementation(project(":samples:calls-shared"))

    // MoodHood REST client — backs this sample's ConferenceBackend implementation.
    implementation(project(":moodhood-api"))

    // Telephony integration (system dialer + self-managed Core-Telecom flows).
    implementation(libs.androidx.core.telecom)

    // Foreground call service lifecycle.
    implementation(libs.androidx.lifecycle.service)

    // MainActivity extends AppCompatActivity.
    implementation(libs.androidx.appcompat)

    // FCM push-initiated calls + token retrieval.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Jetpack Compose UI (CallActivity, screens, components).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
}
