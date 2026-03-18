package space.livedigital.example.calls.entities

import android.telecom.DisconnectCause
import kotlin.time.TimeMark

internal sealed class CallState(open val call: Call) {

    data object Idle : CallState(call = Call.Idle)

    data class Incoming(
        override val call: Call
    ) : CallState(call)

    data class Outgoing(
        override val call: Call,
        val isMuted: Boolean,
    ) : CallState(call)

    data class Active(
        override val call: Call,
        val isMuted: Boolean,
        val startTimeMark: TimeMark
    ) : CallState(call)

    data class Answered(
        override val call: Call,
        val isMuted: Boolean,
    ) : CallState(call)

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

