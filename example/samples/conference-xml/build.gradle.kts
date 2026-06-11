plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "space.livedigital.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "space.livedigital.example.conference.xml"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Shared SDK integration infrastructure (MoodHood REST, engine DI, entities).
    // Transitively exposes the livedigital SDK and Koin.
    implementation(project(":shared"))

    // XML-UI specific dependencies.
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)

    implementation(libs.sequenia.permissionChecker)
}
