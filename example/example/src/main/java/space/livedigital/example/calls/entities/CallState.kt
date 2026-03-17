package space.livedigital.example.calls.entities

import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize
import kotlin.time.TimeMark

sealed class CallState(open val call: Call) {

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

@Parcelize
sealed class Call(
    open val displayName: String,
    open val phone: String,
    open val roomAlias: String
) : Parcelable {

    data object Idle : Call(displayName = "", phone = "", roomAlias = "")

    data class Actual(
        override val displayName: String,
        override val phone: String,
        override val roomAlias: String
    ) : Call(displayName, phone, roomAlias)
}

sealed interface CallAction : Parcelable {
    @Parcelize
    data class Answer(
        val displayName: String,
        val phone: String,
        val roomAlias: String
    ) : CallAction

    @Parcelize
    data class Disconnect(
        val displayName: String,
        val phone: String,
        val roomAlias: String,
        val cause: DisconnectCause
    ) : CallAction

    @Parcelize
    data class Activate(
        val displayName: String,
        val phone: String,
        val roomAlias: String
    ) : CallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : CallAction

    @Parcelize
    data class PlaceIncomingCall(
        val displayName: String,
        val phone: String,
        val roomAlias: String
    ) : CallAction

    @Parcelize
    data class PlaceOutgoingCall(
        val displayName: String,
        val phone: String,
        val roomAlias: String
    ) : CallAction
}
