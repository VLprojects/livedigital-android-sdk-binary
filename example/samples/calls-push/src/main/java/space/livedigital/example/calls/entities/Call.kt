package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// This sample only makes external (PSTN) calls, which are audio-only — there is no
// CallType here on purpose.
@Parcelize
sealed class Call(
    open val displayName: String,
    open val phone: String,
    open val signalingToken: String
) : Parcelable {

    data object Idle : Call(displayName = "", phone = "", signalingToken = "")

    data class Actual(
        override val displayName: String,
        override val phone: String,
        override val signalingToken: String
    ) : Call(displayName, phone, signalingToken)
}