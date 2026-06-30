plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "space.livedigital.example"
    compileSdk = 36

    defaultConfig {
        // Same applicationId as :samples:calls so the existing Firebase google-services.json
        // keeps working. The two call samples therefore can't be installed side by side.
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
    // Shared call infrastructure (telephony, services, CallViewModel + ConferenceBackend, UI).
    // Transitively exposes :shared (SDK + Koin), :design and Compose.
    implementation(project(":calls-core"))

    // Push-device registration REST client — used by this sample's ConferenceBackend (stub for now).
    implementation(project(":devices-api"))

    // MainActivity extends AppCompatActivity.
    implementation(libs.androidx.appcompat)

    // FCM token retrieval in MainActivity.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Jetpack Compose UI used by MainActivity.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
}
