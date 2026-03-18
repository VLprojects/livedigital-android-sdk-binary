package space.livedigital.example.calls.entities

import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize

internal sealed interface CallAction : Parcelable {
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