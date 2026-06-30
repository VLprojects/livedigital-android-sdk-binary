pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
        maven {
            setUrl("https://raw.github.com/VLprojects/livedigital-android-sdk-binary/master")
        }
    }
}

rootProject.name = "LiveDigitalSDK"

// Shared SDK integration infrastructure (MoodHood REST client, engine DI, entities).
include(":shared")

// Standalone REST client for the push-device registration API (PUT/DELETE v1/devices).
include(":devices-api")

// MoodHood REST client (guest auth → room → participant → signaling token → join).
include(":moodhood-api")

// Shared Compose design system used by the Compose samples (conference-compose, calls).
include(":design")

// Shared call infrastructure (telephony, services, CallViewModel + ConferenceBackend, UI)
// reused by the two call samples (:samples:calls, :samples:calls-push).
include(":calls-core")

// Runnable SDK usage samples — each is an independent, installable app.
include(":samples:conference-xml")
include(":samples:conference-compose")
include(":samples:calls")

// Second call sample: same UI/telephony shell as :samples:calls but backed by :devices-api
// (push-token-service flow) instead of MoodHood. Hosts the phone-number auth/login feature.
include(":samples:calls-push")