package space.livedigital.example.calls.entities

import android.telecom.DisconnectCause
import kotlin.time.TimeMark

sealed class CallState(open val call: Call) {

    /**
     * States in which local media is live (or being set up): the mute toggle applies here,
     * and the engine keeps the microphone source in sync with this flag.
     */
    sealed interface WithMedia {
        val isMuted: Boolean

        fun copyMedia(isMuted: Boolean = this.isMuted): CallState
    }

    data object Idle : CallState(call = Call.Idle)

    data class Incoming(
        override val call: Call
    ) : CallState(call)

    data class Outgoing(
        override val call: Call,
        override val isMuted: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean) = copy(isMuted = isMuted)
    }

    data class Active(
        override val call: Call,
        override val isMuted: Boolean,
        val startTimeMark: TimeMark
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean) = copy(isMuted = isMuted)
    }

    data class Activated(
        override val call: Call,
        override val isMuted: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean) = copy(isMuted = isMuted)
    }

    data class Answered(
        override val call: Call,
        override val isMuted: Boolean
    ) : CallState(call), WithMedia {
        override fun copyMedia(isMuted: Boolean) = copy(isMuted = isMuted)
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
