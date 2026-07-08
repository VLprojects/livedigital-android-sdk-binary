import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("kotlin-parcelize")
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
    (System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: devicesSecrets.getProperty(propertyKey).orEmpty())
        .trim()
        .removeSurrounding("\"")

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
            "LOAD_BALANCER_BASE_URL",
            resolveSecret("loadBalancerBaseUrl", "LOAD_BALANCER_BASE_URL").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "DEVICES_API_KEY",
            resolveSecret("devicesApiKey", "DEVICES_API_KEY").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "SIGNALING_API_BASE_URL",
            resolveSecret("signalingApiBaseUrl", "SIGNALING_API_BASE_URL").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "SIGNALING_TOKEN_KEY",
            resolveSecret("signalingTokenKey", "SIGNALING_TOKEN_KEY").asBuildConfigString()
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
    // SDK integration infra (engine DI, PeerAppData, JsonUtils) + Koin.
    implementation(project(":shared"))

    // Compose design system (theme, base components, modifier extensions).
    implementation(project(":design"))

    // Code shared verbatim with :samples:calls — permissions dashboard, telecom endpoint
    // model, CallType, shared Compose components.
    implementation(project(":calls-shared"))

    // Push-device registration REST client — used by this sample's ConferenceBackend (stub for now).
    implementation(project(":devices-api"))

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
