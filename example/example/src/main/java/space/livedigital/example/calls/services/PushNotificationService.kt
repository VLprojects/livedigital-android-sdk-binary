package space.livedigital.example.calls.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.CallHandler
import space.livedigital.example.calls.telecom.entities.CallFromPush
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Need to send new token to server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val isEndCallPushType = remoteMessage.data.getValue("type") == CALL_END

        if (isEndCallPushType) {
            endCall()
        } else {
            startCall(remoteMessage)
        }
    }

    private fun endCall() {
        val callRepository = CallRepository.instance ?: CallRepository.create(this)
        val telecomCallRepository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create()

        callRepository.endCall()
        telecomCallRepository.endCall()
    }

    private fun startCall(remoteMessage: RemoteMessage) {
        val call = CallFromPush(
            id = remoteMessage.messageId ?: "",
            caller = remoteMessage.data.getValue("caller"),
            callerNumber = remoteMessage.data.getValue("callerNumber"),
            roomAlias = remoteMessage.data.getValue("roomAlias")
        )
        initiateCallService(call)
    }

    private fun initiateCallService(call: CallFromPush) {
        runCatching {
            val callHandler = CallHandler(applicationContext)
            callHandler.startIncomingCall(call)
        }.onFailure { e ->
            Log.d(TAG, "${e.message}")
        }
    }

    companion object {
        private const val TAG = "PushNotificationService"
        private const val CALL_END = "call_end"
    }
}