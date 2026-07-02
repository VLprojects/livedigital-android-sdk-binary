package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Call(
    open val displayName: String,
    open val phone: String,
    open val signalingToken: String,
    open val callType: CallType = CallType.AUDIO
) : Parcelable {

    data object Idle : Call(displayName = "", phone = "", signalingToken = "")

    data class Actual(
        override val displayName: String,
        override val phone: String,
        override val signalingToken: String,
        override val callType: CallType
    ) : Call(displayName, phone, signalingToken, callType)
}

@Parcelize
enum class CallType : Parcelable {
    AUDIO,
    VIDEO
}