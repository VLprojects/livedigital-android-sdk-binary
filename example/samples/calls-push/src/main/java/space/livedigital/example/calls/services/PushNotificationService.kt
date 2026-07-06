package space.livedigital.example.calls.services

import android.telecom.DisconnectCause
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.push.PushTokenListener
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.utils.CallHandler

class PushNotificationService : FirebaseMessagingService(), KoinComponent {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        // Delegated to whichever app bound a PushTokenListener (no-op if none).
        getKoin().getOrNull<PushTokenListener>()?.onNewToken(p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        Log.d(TAG, "remoteMessage data $data")

        val kind = data[KEY_KIND]?.let(PushKind::fromWireValue)
        if (kind == null) {
            Log.w(TAG, "Unknown or missing push kind: ${data[KEY_KIND]}")
            return
        }

        when (kind) {
            PushKind.CALL_START -> handleCallStart(data)
            PushKind.CALL_END ->
                disconnectCurrentCall(DisconnectCause(DisconnectCause.REMOTE))

            PushKind.CALL_ANSWERED ->
                disconnectCurrentCall(DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE))

            PushKind.CALL_DECLINED_BY_CALLEE ->
                disconnectCurrentCall(
                    DisconnectCause(
                        DisconnectCause.REJECTED,
                        CallConstants.REASON_DECLINED_ELSEWHERE
                    )
                )

            PushKind.CALL_CANCELLED ->
                disconnectCurrentCall(DisconnectCause(DisconnectCause.REMOTE))
        }
    }

    private fun handleCallStart(data: Map<String, String>) {
        val signalingToken = data[KEY_SIGNALING_TOKEN]
        if (signalingToken.isNullOrEmpty()) {
            Log.w(TAG, "call_start push without a signaling token — ignored")
            return
        }

        val call = Call.Actual(
            displayName = data[KEY_CALLER].orEmpty(),
            phone = HARDCODED_CALLER_PHONE,
            signalingToken = signalingToken,
            callType = CallType.AUDIO
        )

        val callRepository = CallRepository.instance ?: CallRepository.create()
        val currentState = callRepository.currentCallState.value
        val isSafeToStart = currentState is CallState.Idle ||
                currentState is CallState.Ended ||
                currentState is CallState.Missed
        if (!isSafeToStart) {
            Log.d(TAG, "Ignoring incoming call_start — a call is already in progress")
            return
        }

        runCatching {
            CallHandler(applicationContext).startIncomingCall(call)
        }.onFailure { e ->
            Log.d(TAG, "Failed to start incoming call: ${e.message}")
        }
    }

    private fun disconnectCurrentCall(cause: DisconnectCause) {
        val callRepository = CallRepository.instance ?: CallRepository.create()
        val currentState = callRepository.currentCallState.value
        if (currentState is CallState.Idle ||
            currentState is CallState.Ended ||
            currentState is CallState.Missed
        ) {
            Log.d(TAG, "No active call to disconnect for terminal push")
            return
        }

        callRepository.dispatchCallAction(
            CallAction.Disconnect(call = currentState.call, cause = cause)
        )
    }

    companion object {
        private const val TAG = "PushNotificationService"

        private const val KEY_KIND = "kind"
        private const val KEY_CALLER = "caller"
        private const val KEY_SIGNALING_TOKEN = "signalingToken"

        // The new push contract carries no phone number, but the telephony layer and contact
        // matching still need a tel: address, so we use a fixed placeholder for now.
        private const val HARDCODED_CALLER_PHONE = "+79999999999"
    }

    private enum class PushKind(val wireValue: String) {
        CALL_START("call_start"),
        CALL_END("call_end"),
        CALL_ANSWERED("call_answered"),
        CALL_DECLINED_BY_CALLEE("call_declined_by_callee"),
        CALL_CANCELLED("call_cancelled");

        companion object {
            fun fromWireValue(value: String): PushKind? =
                entries.firstOrNull { it.wireValue == value }
        }
    }
}
