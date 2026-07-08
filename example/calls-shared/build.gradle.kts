plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "space.livedigital.example.calls.shared"
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
    // Design system (theme, button styles, gradientBackground) — part of the public API of
    // the shared composables; :design exposes Compose itself as api.
    api(project(":design"))

    // PermissionsViewModel.
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // CallConverter maps androidx.core.telecom types to the sample-agnostic endpoint model.
    implementation(libs.androidx.core.telecom)

    // @Preview composables in the shared components.
    implementation(libs.androidx.ui.tooling.preview)
}
