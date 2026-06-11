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
    implementation("com.github.vlprojects:livedigital-android-sdk:1.7.0")
}
```

## Integration example and demo

The `example/` folder is a self-contained Gradle project that demonstrates integrating and using the livedigital SDK. It is split into one shared integration module and three independent sample apps that install side by side:

| Module | Description |
|--------|-------------|
| `:shared` | Shared SDK-integration infrastructure (the MoodHood REST flow, the `LiveDigitalEngine` factory, domain entities). No UI. |
| `:samples:conference-xml` | Conference scenario, UI on classic Android Views (XML + `RecyclerView`). |
| `:samples:conference-compose` | The same conference scenario, UI on Jetpack Compose. |
| `:samples:calls` | 1-on-1 calls: Android telephony integration + FCM push-initiated calls (Jetpack Compose UI). |

Each module has its own README, and [`docs/SAMPLES.md`](docs/SAMPLES.md) compares the three samples side by side. The samples are intentionally minimalistic and should not be judged in terms of UX convenience, code beauty and architecture, robustness of solutions, etc.
