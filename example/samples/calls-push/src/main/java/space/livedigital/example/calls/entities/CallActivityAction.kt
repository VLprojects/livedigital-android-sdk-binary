package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface CallActivityAction : Parcelable {

    @Parcelize
    data class Answer(val call: Call) : CallActivityAction

    @Parcelize
    data class PlaceMissedCall(val call: Call) : CallActivityAction

    @Parcelize
    data object StartBackgroundAudioService : CallActivityAction
}