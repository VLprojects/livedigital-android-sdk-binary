package space.livedigital.example.calls

import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.sdk.channel.ChannelSessionStatus
import space.livedigital.sdk.media.audio.AudioSource
import kotlin.time.Duration

data class ScreenState(
    val isLocalAudioOn: Boolean = false,
    val localAudioSource: AudioSource? = null,
    val callState: CallState = CallState.Idle,
    val sessionStatus: ChannelSessionStatus = ChannelSessionStatus.STOPPED,
    val callDuration: Duration = Duration.ZERO,
    val isAudioDevicePickerShown: Boolean = false,
    val audioDevices: List<GeneralCallEndpoint> = emptyList(),
    val currentAudioDevice: GeneralCallEndpoint? = null
)
