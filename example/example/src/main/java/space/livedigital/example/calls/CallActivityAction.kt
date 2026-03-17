package space.livedigital.example.calls

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import space.livedigital.example.calls.entities.Call

sealed interface CallActivityAction : Parcelable {

    @Parcelize
    data class Answer(val call: Call) : CallActivityAction

    @Parcelize
    data class OutgoingCall(
        val callerName: String,
        val phoneNumber: String,
        val roomAlias: String
    ) : CallActivityAction
}
