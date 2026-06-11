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
    // Shared SDK integration infrastructure (MoodHood REST, engine DI, entities).
    // Transitively exposes the livedigital SDK and Koin.
    implementation(project(":shared"))

    // Telephony integration (system dialer + self-managed Core-Telecom flows).
    implementation(libs.androidx.core.telecom)

    // FCM push-initiated calls.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Foreground call service lifecycle.
    implementation(libs.androidx.lifecycle.service)

    // Network inspector. Referenced from main source (InterceptorsModule / CallViewModel),
    // so it must be available in every build variant.
    implementation(libs.chucker.library)

    // Shared Compose design system (theme, base components, modifier extensions).
    implementation(project(":design"))

    // Jetpack Compose UI.
    implementation(libs.androidx.ui)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
}
