package space.livedigital.example.telecom_calls.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

class CallConnection(private val context: Context, val call: Call) : Connection() {

    private val TAG = "CallConnection"

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        if (state == STATE_ACTIVE) {
            Handler(Looper.getMainLooper()).postDelayed({
                context.sendBroadcast(
                    Intent(context, CallAnsweredReceiver::class.java).apply {
                        action = CallAnsweredReceiver.ACTION_CALL_ANSWERED
                        putExtra(CallAnsweredReceiver.ROOM_ALIAS_KEY, call.roomAlias)
                    }
                )
            }, 300)
        }
        Log.d(TAG, "onStateChanged: $state")
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.d(TAG, "onDisconnect")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL, "disconnected"))
        TelecomCallRepository.endCall()
    }

    override fun onAnswer() {
        Log.d("xd", "onAnswer")
        setActive()
    }

    override fun onReject() {
        Log.d(TAG, "onReject without params")
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, "Rejected"))
        TelecomCallRepository.endCall()
    }

    override fun onAbort() {
        super.onAbort()
        Log.e(TAG, "OnAbort")
    }

    override fun onReject(rejectReason: Int) {
        super.onReject(rejectReason)
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, rejectReason.toString()))
        TelecomCallRepository.endCall()
    }

    override fun onReject(replyMessage: String?) {
        super.onReject(replyMessage)
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE, replyMessage))
        TelecomCallRepository.endCall()
    }
}