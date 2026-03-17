package space.livedigital.example.calls.services

import android.telecom.DisconnectCause
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.CallHandler
import space.livedigital.example.calls.telecom.entities.CallFromPush

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Need to send new token to server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val pushType = PushType.valueOf(remoteMessage.data.getValue("type").uppercase())
        val call = CallFromPush(
            id = remoteMessage.messageId ?: "",
            caller = remoteMessage.data.getValue("caller"),
            callerNumber = remoteMessage.data.getValue("callerNumber"),
            roomAlias = remoteMessage.data.getValue("roomAlias")
        )

        when (pushType) {
            PushType.CALL_START -> startCall(call)
            PushType.CALL_END -> endCall(call)
            PushType.CALL_ANSWERED -> onCallAnswered(call)
        }
    }

    private fun startCall(call: CallFromPush) {
        runCatching {
            val callHandler = CallHandler(applicationContext)
            callHandler.startIncomingCall(call)
        }.onFailure { e ->
            Log.d(TAG, "${e.message}")
        }
    }

    private fun endCall(call: CallFromPush) {
        val callRepository = CallRepository.instance ?: CallRepository.create()
        callRepository.dispatchCallAction(
            CallAction.Disconnect(
                displayName = call.caller,
                phone = call.callerNumber,
                roomAlias = call.roomAlias,
                cause = DisconnectCause(DisconnectCause.REMOTE)
            )
        )
    }

    fun onCallAnswered(call: CallFromPush) {
        val callRepository = CallRepository.instance ?: CallRepository.create()
        callRepository.dispatchCallAction(
            CallAction.Answer(
                displayName = call.caller,
                phone = call.callerNumber,
                roomAlias = call.roomAlias
            )
        )
    }

    companion object {
        private const val TAG = "PushNotificationService"
    }

    private enum class PushType() {
        CALL_START,
        CALL_END,
        CALL_ANSWERED
    }
}