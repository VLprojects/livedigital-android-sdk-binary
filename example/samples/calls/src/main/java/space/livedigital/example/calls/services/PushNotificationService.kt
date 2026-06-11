package space.livedigital.example.calls.services

import android.telecom.DisconnectCause
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.utils.CallHandler

internal class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Need to send new token to server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val pushType = PushType.valueOf(remoteMessage.data.getValue("type").uppercase())
        val call = Call.Actual(
            displayName = remoteMessage.data.getValue("caller"),
            phone = remoteMessage.data.getValue("callerNumber"),
            roomAlias = remoteMessage.data.getValue("roomAlias"),
            callType = CallType.valueOf(remoteMessage.data.getValue("callType").uppercase())
        )

        val callRepository = CallRepository.instance ?: CallRepository.create()
        val currentState = callRepository.currentCallState.value
        val isSafeToStart = currentState is CallState.Missed ||
                currentState is CallState.Ended ||
                currentState is CallState.Idle

        if (pushType == PushType.CALL_START && !isSafeToStart) {
            callRepository.dispatchCallAction(
                CallAction.Disconnect(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    cause = DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE),
                    callType = call.callType
                )
            )
        }

        when (pushType) {
            PushType.CALL_START -> startCall(call)
            PushType.CALL_END -> endCall(call)
            PushType.CALL_ANSWERED -> onCallAnswered(call)
        }
    }

    private fun startCall(call: Call) {
        runCatching {
            val callHandler = CallHandler(applicationContext)
            callHandler.startIncomingCall(call)
        }.onFailure { e ->
            Log.d(TAG, "${e.message}")
        }
    }

    private fun endCall(call: Call) {
        val callRepository = CallRepository.instance ?: CallRepository.create()
        callRepository.dispatchCallAction(
            CallAction.Disconnect(
                displayName = call.displayName,
                phone = call.phone,
                roomAlias = call.roomAlias,
                cause = DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE),
                callType = call.callType
            )
        )
    }

    private fun onCallAnswered(call: Call) {
        val callRepository = CallRepository.instance ?: CallRepository.create()
        callRepository.dispatchCallAction(
            CallAction.Activate(
                displayName = call.displayName,
                phone = call.phone,
                roomAlias = call.roomAlias,
                callType = call.callType
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