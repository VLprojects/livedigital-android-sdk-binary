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

// Shared Compose design system used by the Compose samples (conference-compose, calls).
include(":design")

// Runnable SDK usage samples — each is an independent, installable app.
include(":samples:conference-xml")
include(":samples:conference-compose")
include(":samples:calls")