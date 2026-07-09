# Sample: Calls (push-token-service)

A second "calls" example on top of the livedigital SDK. It reuses the same UI and telephony shell as
[`:samples:calls`](../calls) but is backed by the **push-device registration API**
([`:devices-api`](../../devices-api)) instead of MoodHood: the user signs in with a phone number,
the
device is registered for FCM push, and calls (incoming via push, outgoing via a locally-minted
signaling token) are wired to Android telephony. UI built with **Jetpack Compose**.

`applicationId`: `space.livedigital.example` (kept so the existing Firebase project keeps working —
see below).

## What it demonstrates

* **Phone-number sign-in + device registration** (`AuthViewModel`, `device/DeviceRepository`): on
  sign-in the device is registered with the Devices API by `deviceId`, current FCM token, phone
  number, locale, device name and timezone; on sign-out it is unregistered. Registration is re-run
  on app start (`DeviceRepository.registerIfSignedIn`) and whenever FCM rotates the token
  (`device/DeviceTokenListener` ← `PushNotificationService.onNewToken`).
* **Incoming calls via FCM data push** (`calls/services/PushNotificationService`): the push contract
  carries a `kind` (`call_start` / `call_end` / `call_answered` / `call_declined_by_callee` /
  `call_cancelled`) and, for `call_start`, a ready-to-use `signalingToken`. Terminal kinds map to a
  telecom `DisconnectCause`.
* **Outgoing calls with a locally-minted token** (`calls/utils/OutgoingTokenGenerator` →
  `OutboundCallTokenGenerator`): the app signs an HS256 JWT carrying `externalCall` outbound claims
  (caller/callee, tenant derived from `DEVICES_API_KEY`) with `SIGNALING_TOKEN_KEY`. Dialing from
  the
  app's own UI (`ui/screens/OutgoingCallSection`) routes through `CallHandler.placeOutgoingCall`;
  numbers redialed from the system call log are re-minted in `CallHandler.buildOutgoingCall`.
* **Originating the PSTN leg**: after joining the channel, an outbound session calls
  `engine.initiateCall()` exactly once (`CallViewModel.initiateOutboundCallIfNeeded`, keyed off
  `OutboundCallTokenGenerator.isOutboundCallToken`). Incoming (push-delivered) tokens skip this.
* **Two interchangeable telephony flows**, chosen at runtime (`calls/utils/CallHandler.kt`) — shared
  verbatim with `:samples:calls` via [`:calls-shared`](../../calls-shared):
    * **System dialer integration** — via `ConnectionService` and a registered `PhoneAccount`
      (`CallConnectionService`; the account is registered in `App.onCreate`). Requires phone
      permission and enabling the calling account in system settings.
    * **Self-managed (Core-Telecom)** — a fallback via `androidx.core.telecom.CallsManager`
      (`CallService`) when the conditions for system integration are not met.
* A single source of truth for call state — `CallRepository` (`MutableStateFlow<CallState>`), a
  `CallAction` → `CallState` reducer.
* SDK session management in `CallViewModel` (channel connect, local audio, the `LiveDigitalEngine`
  lifecycle).

The SDK integration infrastructure (engine DI, `PeerAppData`, `JsonUtils`) lives in the
[`:shared`](../../shared) module; the permissions dashboard, telecom endpoint model and shared
Compose components in [`:calls-shared`](../../calls-shared).

## Required: google-services.json

The sample uses Firebase Cloud Messaging. Place a `google-services.json` file in the module root:

```
example/samples/calls-push/google-services.json
```

The file is **not stored in the repository** (see `.gitignore`). The current Firebase project
(`livedigital-sdk-example`) is registered for the package `space.livedigital.example` — which is why
the sample's `applicationId` is kept as the original. If you use your own Firebase project, register
an app with this package in it or change the `applicationId` in `build.gradle.kts`.

For system integration to work, grant the app phone permission and enable it as a calling account in
settings. Without that, calls go through the self-managed flow.

## Required: Devices API configuration

Unlike `:samples:calls`, this sample talks to the push-device registration and signaling backends,
configured through five values surfaced as `BuildConfig` fields:

| `BuildConfig` field      | `secrets.properties` key | env var                  | used for                                                            |
|--------------------------|--------------------------|--------------------------|---------------------------------------------------------------------|
| `DEVICES_BASE_URL`       | `devicesBaseUrl`         | `DEVICES_BASE_URL`       | Devices API base URL (register/unregister device)                   |
| `DEVICES_API_KEY`        | `devicesApiKey`          | `DEVICES_API_KEY`        | Devices API key; its `tenantId` prefix is reused in outbound tokens |
| `LOAD_BALANCER_BASE_URL` | `loadBalancerBaseUrl`    | `LOAD_BALANCER_BASE_URL` | engine load-balancer endpoint                                       |
| `SIGNALING_API_BASE_URL` | `signalingApiBaseUrl`    | `SIGNALING_API_BASE_URL` | signaling API endpoint                                              |
| `SIGNALING_TOKEN_KEY`    | `signalingTokenKey`      | `SIGNALING_TOKEN_KEY`    | HMAC secret used to sign locally-minted outbound tokens             |

Resolution order (see `build.gradle.kts`): the environment variable first, then
`secrets.properties`,
then the committed placeholder defaults in `secrets.defaults.properties`. The committed defaults are
blank so the project always builds; to use real values, copy the defaults file to
`secrets.properties`
(gitignored) and fill them in, or set the environment variables (e.g. in CI).

> Note: `BuildConfig` values are embedded in the APK and are extractable — do not treat them as
> secrets in a production app.

## Push contract

Incoming calls are delivered as FCM data messages. Once the device is registered (phone-number
sign-in), the backend addresses pushes to the device's current FCM token; each message carries a
`kind` field:

* `call_start` — must also include a `signalingToken` (and optionally a `caller` display name).
* `call_end` / `call_answered` / `call_declined_by_callee` / `call_cancelled` — terminate the
  current
  call.
