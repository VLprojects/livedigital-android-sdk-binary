# Sample: Conference (XML UI)

A livedigital SDK conference example with a UI built on classic Android Views (XML +
`RecyclerView`).

`applicationId`: `space.livedigital.example.conference.xml`

## What it demonstrates

* The basic conference scenario: authenticating with MoodHood, joining a room, connecting to a
  channel via `LiveDigitalEngine.connectToChannel(...)`.
* Displaying participants' video in a grid on a `RecyclerView` (`RemotePeerAdapter`,
  `PeerDiffUtils`, `DiffUtilsUpdater`).
* A Foreground Service `CallService` for running in the background (starting with Android 15, the
  system restricts network requests from inactive processes).
* Turning off the local camera when the app goes into the background (Android limits camera usage in
  the background to ~5 seconds).

All SDK work is concentrated in `MainActivity` and `MainViewModel`. The shared integration code (the
MoodHood REST client, the engine factory, the entities) lives in the [`:shared`](../../shared)
module.

## Running

```bash
cd example
./gradlew :samples:conference-xml:installDebug
```

The group and room are hardcoded in the `companion object` of `MainViewModel.kt`. To join from the
web version, see the link in the comment above the `MainActivity` class. **To successfully join, a
user from the web version must be present in the room.**

## In-room actions

* **Restart** — reconnect to the room.
* **Camera** — turn the camera on/off (the first tap requests permission).
* **Mic** — turn the microphone on/off (the first tap requests permission).
* Switch the audio device (the first tap requests Bluetooth permission).
* **Switch camera** — switch between the front and back cameras.

The local camera video is in the bottom-right corner of the grid; a remote participant's video fills
the whole grid. Swiping left/right switches between remote participants.

> Handling of permission denials is not implemented in the sample.
