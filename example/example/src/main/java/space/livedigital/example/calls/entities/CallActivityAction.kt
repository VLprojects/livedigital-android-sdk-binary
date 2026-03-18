package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface CallActivityAction : Parcelable {

    @Parcelize
    data class Answer(val call: Call) : CallActivityAction

    @Parcelize
    data class PlaceOutgoingCall(
        val callerName: String,
        val phoneNumber: String,
        val roomAlias: String
    ) : CallActivityAction

    @Parcelize
    data object StartBackgroundAudioService : CallActivityAction
}