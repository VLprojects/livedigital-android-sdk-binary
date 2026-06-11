package space.livedigital.example.calls

import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.sdk.channel.ChannelSessionStatus
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.media.audio.AudioSource
import space.livedigital.sdk.media.video.CameraPosition
import space.livedigital.sdk.media.video.VideoSource
import kotlin.time.Duration
import kotlin.time.TimeMark

internal data class ScreenState(
    val remotePeers: List<PeerWithVersion> = emptyList(),
    val isLocalVideoOn: Boolean = false,
    val localVideoSource: VideoSource? = null,
    val localCameraPosition: CameraPosition = CameraPosition.BACK,
    val isLocalAudioOn: Boolean = false,
    val localAudioSource: AudioSource? = null,
    val callState: CallState = CallState.Idle,
    val sessionStatus: ChannelSessionStatus = ChannelSessionStatus.STOPPED,
    val callDuration: Duration = Duration.ZERO,
    val isLocalPreviewFullscreen: Boolean = false,
    val isAudioDevicePickerShown: Boolean = false,
    val audioDevices: List<GeneralCallEndpoint> = emptyList(),
    val currentAudioDevice: GeneralCallEndpoint? = null
)

data class PeerWithVersion(
    val peer: Peer,
    val version: TimeMark
)
