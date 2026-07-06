package space.livedigital.example.calls.entities

import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize

sealed interface CallAction : Parcelable {

    @Parcelize
    data class Answer(
        val call: Call,
        val isMuted: Boolean,
        val isCameraOn: Boolean
    ) : CallAction

    @Parcelize
    data class Disconnect(
        val call: Call,
        val cause: DisconnectCause
    ) : CallAction

    @Parcelize
    data class Activate(val call: Call) : CallAction

    @Parcelize
    data class PlaceActiveCall(val call: Call) : CallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : CallAction

    @Parcelize
    data class ToggleCamera(val isCameraOn: Boolean) : CallAction

    @Parcelize
    data class PlaceIncomingCall(val call: Call) : CallAction

    @Parcelize
    data class PlaceOutgoingCall(
        val call: Call,
        val isMuted: Boolean,
        val isCameraOn: Boolean
    ) : CallAction

    @Parcelize
    data class PlaceMissedCall(val call: Call) : CallAction
}
