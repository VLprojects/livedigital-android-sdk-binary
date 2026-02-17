package space.livedigital.example.calls.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.livedigital.example.calls.telecom.CallHandler
import space.livedigital.example.calls.telecom.entities.CallFromPush

class PushNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val call = CallFromPush(
            id = remoteMessage.messageId ?: "",
            caller = remoteMessage.data.getValue("caller"),
            callerNumber = remoteMessage.data.getValue("callerNumber"),
            roomAlias = remoteMessage.data.getValue("roomAlias")
        )

        initiateCallService(call)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Need to send new token to server
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
    }
}