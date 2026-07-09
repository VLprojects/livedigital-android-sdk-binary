# livedigital SDK

This repository contains builds of the native livedigital SDK implementation for Android, distributed as a fat binary.

The livedigital SDK is a client for the livedigital service (https://docs.livedigital.space). The SDK implements:
  * signaling (connecting and exchanging commands with the media server)
  * connection recovery logic on reconnect
  * connection quality analysis
  * handling of incoming and outgoing media tracks
  * working with video layers
  * background blur and replacement for outgoing video
  * active-speaker detection logic
  * a transport for application-specific commands and metadata
  * initiating and declining calls to external telephony (PSTN) through the SIP gateway

## Compatibility

* Android devices / Android 9+
* Android emulators / Android 9+

Android target sdk = 16 (api 36)

The SDK works on emulators, but full testing should be done on a real device, since the camera implementation, background blur, and the set of supported codecs and video formats may differ.

## Dependencies

### Kotlin version

Kotlin version 2.1.20

### Libraries

The SDK uses the following dependencies:
| Library | Group:Artifact | Version |
|---------|----------------|---------|
| Kotlinx Serialization JSON | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.0 |
| Retrofit | `com.squareup.retrofit2:retrofit` | 2.11.0 |
| Retrofit Serialization Converter | `com.squareup.retrofit2:converter-kotlinx-serialization` | 2.11.0 |
| OkHttp3 Logging Interceptor | `com.squareup.okhttp3:logging-interceptor` | 4.12.0 |
| Socket.IO Client | `io.socket:socket.io-client` | 2.1.2 |
| ML Kit Segmentation Selfie | `com.google.mlkit:segmentation-selfie` | 16.0.0-beta6 |
| Protobuf Kotlin Lite | `com.google.protobuf:protobuf-kotlin-lite` | 4.33.3 |

## Integration

Integration is done through a Maven repository.

1. Add the following to your project's `build.gradle`:

```
dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://raw.github.com/VLprojects/livedigital-android-sdk-binary/master")
        }
    }
}
```

2. Add the dependency to the `build.gradle` of the relevant module:

```
dependencies {
    implementation("com.github.vlprojects:livedigital-android-sdk:1.8.0")
}
```

## Integration example and demo

The `example/` folder is a self-contained Gradle project that demonstrates integrating and using the livedigital SDK. It is split into shared library modules and four independent sample apps that install side by side.

Shared library modules (no launchable app):

| Module | Description |
|--------|-------------|
| `:shared` | livedigital SDK integration glue: the Koin `LiveDigitalEngine` factory, a console logger and shared helpers. Transitively re-exports the SDK and Koin to the samples. No UI. |
| `:moodhood-api` | REST client for the MoodHood backend — guest authentication, rooms, participants and signaling tokens. Used by the conference samples and `:samples:calls`. |
| `:devices-api` | REST client for the CRS push-device-registration API — registering/unregistering a device's push token against a phone number. Used by `:samples:calls-push`. |
| `:calls-shared` | Shared telephony shell for the call samples: Android Telecom integration, runtime permissions, call entities, contacts and push-token plumbing. Used by `:samples:calls` and `:samples:calls-push`. |
| `:design` | Shared Jetpack Compose design system (theme, colors, typography, buttons). Used by the Compose-based modules. |

Sample apps:

| Module | Description |
|--------|-------------|
| `:samples:conference-xml` | Conference scenario, UI on classic Android Views (XML + `RecyclerView`). |
| `:samples:conference-compose` | The same conference scenario, UI on Jetpack Compose. |
| `:samples:calls` | 1-on-1 phone calls integrated with Android telephony (`ConnectionService` and Core-Telecom), with call setup over MoodHood and FCM push. Jetpack Compose UI. |
| `:samples:calls-push` | The same calls shell, but backed by the CRS push-device-registration API and the SIP gateway: phone-number sign-in, device registration for push, incoming calls delivered via push and outgoing calls placed with a signaling token and `initiateCall()`. Jetpack Compose UI. |

Each sample app has its own README. The samples are intentionally minimalistic and should not be judged in terms of UX convenience, code beauty and architecture, robustness of solutions, etc.
