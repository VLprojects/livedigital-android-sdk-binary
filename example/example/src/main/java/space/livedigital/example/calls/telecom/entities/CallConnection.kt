package space.livedigital.example.calls.telecom.entities

import android.telecom.Connection
import android.telecom.DisconnectCause
import space.livedigital.example.calls.entities.Call

class CallConnection(private val call: Call) : Connection() {

    private val listeners = mutableListOf<CallStateListener>()

    fun addListener(listener: CallStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CallStateListener) {
        listeners.remove(listener)
    }

    override fun onDisconnect() {
        super.onDisconnect()
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.LOCAL)
            )
        }
    }

    override fun onAnswer() {
        super.onAnswer()
        listeners.forEach {
            it.onAnswer(
                call = call
            )
        }
    }

    override fun onReject() {
        super.onReject()
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    override fun onReject(rejectReason: Int) {
        super.onReject(rejectReason)
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    override fun onReject(replyMessage: String?) {
        super.onReject(replyMessage)
        listeners.forEach {
            it.onDisconnect(
                call = call,
                disconnectCause = DisconnectCause(DisconnectCause.REJECTED)
            )
        }
    }

    interface CallStateListener {

        fun onAnswer(call: Call)

        fun onDisconnect(call: Call, disconnectCause: DisconnectCause)
    }

    companion object {
        private const val TAG = "CallConnection"
    }
}