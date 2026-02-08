package space.livedigital.example.telecom_calls.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.livedigital.example.calls.utils.CallRepository
import space.livedigital.example.calls.utils.CallService

class PushNotificationService : FirebaseMessagingService() {

    private val callRepository by lazy {
        CallRepository.instance ?: CallRepository.create(
            applicationContext
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val call = Call(
            id = remoteMessage.messageId ?: "",
            caller = remoteMessage.data.getValue("caller"),
            callerNumber = remoteMessage.data.getValue("callerNumber"),
            roomAlias = remoteMessage.data.getValue("roomAlias")
        )
        initiateCallService(call)
    }

    override fun onNewToken(p0: String) {
        Log.d("xd", "onNewToken $p0")
        super.onNewToken(p0)
    }

    private fun initiateCallService(call: Call) {
        runCatching {
            Log.d(TAG, call.toString())
            val callHandler = CallHandler()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                launchCall(
                    action = CallService.ACTION_INCOMING_CALL,
                    name = call.caller,
                    phoneNumber = call.callerNumber,
                    roomAlias = call.roomAlias
                )
            }

            val isIncomingCallStarted =
                callHandler.startIncomingCall(context = applicationContext, call = call)

            if (isIncomingCallStarted) return

            launchCall(
                action = CallService.ACTION_INCOMING_CALL,
                name = call.caller,
                phoneNumber = call.callerNumber,
                roomAlias = call.roomAlias
            )

        }.onFailure { e ->
            Log.d(TAG, "${e.message}")
        }
    }

    companion object {
        private const val TAG = "PushNotificationService"
    }

    private fun Context.launchCall(
        action: String,
        name: String,
        phoneNumber: String,
        roomAlias: String
    ) {
        startForegroundService(
            Intent(this, CallService::class.java).apply {
                this.action = action
                putExtra(CallService.EXTRA_NAME, name)
                putExtra(CallService.EXTRA_NUMBER, phoneNumber)
                putExtra(CallService.EXTRA_ROOM_ALIAS, roomAlias)
            },
        )
    }
}