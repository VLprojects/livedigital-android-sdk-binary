import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val devicesSecrets = Properties().apply {
    val local = file("secrets.properties")
    val source = if (local.exists()) {
        local
    } else {
        file("secrets.defaults.properties")
    }
    if (source.exists()) {
        source.inputStream().use {
            load(it)
        }
    }
}

fun resolveSecret(propertyKey: String, envName: String): String =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: devicesSecrets.getProperty(propertyKey).orEmpty()

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "space.livedigital.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.livedigital.example"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "DEVICES_BASE_URL",
            resolveSecret("devicesBaseUrl", "DEVICES_BASE_URL").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "DEVICES_API_KEY",
            resolveSecret("devicesApiKey", "DEVICES_API_KEY").asBuildConfigString()
        )
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

    buildFeatures {
        buildConfig = true
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
