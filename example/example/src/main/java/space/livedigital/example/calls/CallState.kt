package space.livedigital.example.calls

import android.os.ParcelUuid
import android.os.Parcelable
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.parcelize.Parcelize
import space.livedigital.sdk.media.audio.AudioRoute

sealed interface CallState {
    object None : CallState

    /**
     * Represents a registered call with the telecom stack with the values provided by the
     * Telecom SDK
     */
    data class Registered(
        val callAttributes: CallAttributesCompat,
        val isMuted: Boolean,
        val roomAlias: String,
        val errorCode: Int?,
        val isOnHold: Boolean,
        val isActive: Boolean,
        internal val actionSource: Channel<CallAction>,
    ) : CallState {
        fun isIncoming() = callAttributes.direction == CallAttributesCompat.DIRECTION_INCOMING

        fun processAction(action: CallAction): Boolean = actionSource.trySend(action).isSuccess
    }

    /**
     * Represent a previously registered call that was disconnected
     */
    data class Unregistered(
        val callAttributes: CallAttributesCompat,
        val disconnectCause: DisconnectCause,
    ) : CallState
}

sealed interface CallAction : Parcelable {
    @Parcelize
    object Answer : CallAction

    @Parcelize
    data class Disconnect(val cause: DisconnectCause) : CallAction

    @Parcelize
    object Activate : CallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : CallAction
}
