package space.livedigital.example.calls.entities

import android.telecom.DisconnectCause
import kotlin.time.TimeMark

sealed class CallState(open val call: Call) {

    /**
     * States in which local media is live (or being set up): mute/camera toggles apply here,
     * and the engine keeps the microphone/camera sources in sync with these flags.
     */
    sealed interface WithMedia {
        val isMuted: Boolean
        val isCameraOn: Boolean

        fun copyMedia(
            isMuted: Boolean = this.isMuted,
            isCameraOn: Boolean = this.isCameraOn
        ): CallState
    }

    data object Idle : CallState(call = Call.Idle)

    data class Incoming(
        override val call: Call
    ) : CallState(call)

    data class Outgoing(
        override val call: Call,
        override val isMuted: Boolean,
        override val isCameraOn: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean, isCameraOn: Boolean) =
            copy(isMuted = isMuted, isCameraOn = isCameraOn)
    }

    data class Active(
        override val call: Call,
        override val isMuted: Boolean,
        override val isCameraOn: Boolean,
        val startTimeMark: TimeMark
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean, isCameraOn: Boolean) =
            copy(isMuted = isMuted, isCameraOn = isCameraOn)
    }

    data class Activated(
        override val call: Call,
        override val isMuted: Boolean,
        override val isCameraOn: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean, isCameraOn: Boolean) =
            copy(isMuted = isMuted, isCameraOn = isCameraOn)
    }

    data class Answered(
        override val call: Call,
        override val isMuted: Boolean,
        override val isCameraOn: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean, isCameraOn: Boolean) =
            copy(isMuted = isMuted, isCameraOn = isCameraOn)
    }

    data class Missed(
        override val call: Call,
        val disconnectCause: DisconnectCause
    ) : CallState(call)

    data class Ended(
        override val call: Call,
        val wasActive: Boolean,
        val disconnectCause: DisconnectCause
    ) : CallState(call)
}
