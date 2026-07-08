package space.livedigital.example.calls.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CallType : Parcelable {
    AUDIO,
    VIDEO
}
