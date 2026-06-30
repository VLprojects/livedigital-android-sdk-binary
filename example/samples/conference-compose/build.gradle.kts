plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "space.livedigital.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.livedigital.example.conference.compose"
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
    // Shared SDK integration infrastructure (engine DI, PeerAppData, JsonUtils).
    // Transitively exposes the livedigital SDK and Koin.
    implementation(project(":shared"))

    // MoodHood REST client (guest auth → room → participant → signaling token → join).
    implementation(project(":moodhood-api"))

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
