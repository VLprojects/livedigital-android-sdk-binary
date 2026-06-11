# Sample: Conference (Compose UI)

The same conference scenario as in [`conference-xml`](../conference-xml), but with a UI built in *
*Jetpack Compose**.

`applicationId`: `space.livedigital.example.conference.compose`

## What it demonstrates

* The basic conference scenario: authenticating with MoodHood, joining a room, connecting to a
  channel via `LiveDigitalEngine.connectToChannel(...)`.
* Rendering participants and controls in Compose (`MainScreen.kt`).
* A small Compose design system in the `ui/` package (`ui/theme`, `ui/components`). It is
  intentionally local to the sample: the design-system versions in `conference-compose` and `calls`
  have diverged, so they are not extracted into a shared module.
* A Foreground Service `CallService` for running in the background, and turning off the local camera
  when going into the background.

The SDK logic lives in `MainViewModel`. The shared integration code is in the [
`:shared`](../../shared) module.

## Running

```bash
cd example
./gradlew :samples:conference-compose:installDebug
```

The group and room are hardcoded in the `companion object` of `MainViewModel.kt`. **To successfully
join, a user from the web version must be present in the room.**

## In-room actions

The set of actions matches the `conference-xml` sample: reconnect, turn the camera and microphone
on/off, select the audio device, switch the camera. Handling of permission denials is not
implemented in the sample.
