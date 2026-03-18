package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class Call(
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