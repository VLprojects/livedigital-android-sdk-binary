# Sample: Calls

A "calls" example on top of the livedigital SDK: incoming/outgoing calls integrated with Android
telephony, and call initiation via FCM push. UI built with **Jetpack Compose**.

`applicationId`: `space.livedigital.example` (the original id is kept so the existing Firebase
project keeps working — see below).

## What it demonstrates

* Two interchangeable telephony flows, chosen at runtime (`calls/utils/CallHandler.kt`):
    * **System dialer integration** — via `ConnectionService` and a registered `PhoneAccount` (
      `CallConnectionService`). Requires phone permission and enabling the calling account in system
      settings.
    * **Self-managed (Core-Telecom)** — a fallback via `androidx.core.telecom.CallsManager` (
      `CallService`) when the conditions for system integration are not met.
* A single source of truth for call state — `CallRepository` (`MutableStateFlow<CallState>`), a
  `CallAction` → `CallState` reducer.
* Call initiation via FCM data push (`PushNotificationService`, types `CALL_START` / `CALL_END` /
  `CALL_ANSWERED`).
* SDK session management in `CallViewModel` (starting/stopping local audio/video, the
  `LiveDigitalEngine` lifecycle).
* Network inspector [Chucker](https://github.com/ChuckerTeam/chucker): the interceptor is provided
  through `di/InterceptorsModule` and passed to the shared `MoodHoodApiClient` via the
  `additionalInterceptors` parameter.

The shared SDK integration code lives in the [`:shared`](../../shared) module.

## Required: google-services.json

The sample uses Firebase Cloud Messaging. Place a `google-services.json` file in the module root:

```
example/samples/calls/google-services.json
```

The file is **not stored in the repository** (see `.gitignore`). The current Firebase project (
`livedigital-sdk-example`) is registered for the package `space.livedigital.example` — which is why
the sample's `applicationId` is kept as the original. If you use your own Firebase project, register
an app with this package in it or change the `applicationId` in `build.gradle.kts`.

For system integration to work, grant the app phone permission and enable it as a calling account in
settings. Without that, calls go through the self-managed flow.

## Sending push (App.py)

`App.py` is a desktop tkinter utility for sending FCM pushes to a device.

* It needs a `serviceAccount.json` (a Firebase service account key) next to `App.py` — not stored in
  the repository.
* Run: `python App.py`. Fill in the FCM token, caller name/number, room alias, and call type, then
  send `CALL_START` / `CALL_END` / `CALL_ANSWERED`.
