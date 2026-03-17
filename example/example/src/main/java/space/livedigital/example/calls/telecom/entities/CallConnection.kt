package space.livedigital.example.calls.telecom.entities

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState

class CallConnection(
    scope: CoroutineScope,
    private val call: CallFromPush
) : Connection() {

    private val TAG = "CallConnection"

    private val listeners = mutableListOf<CallStateListener>()
    private val actionSource = Channel<CallAction>()

    init {
//        scope.launch {
//            actionSource.consumeAsFlow().collect { action ->
//                when (action) {
//                    is CallAction.Disconnect -> {
//                        closeConnection(action.cause)
//                    }
//
//                    is CallAction.ToggleMute -> {
//                        listeners.forEach {
//                            it.onMuteStatusChanged(action.isMute)
//                        }
//                    }
//
//                    CallAction.Activate -> {}
//
//                    CallAction.Answer -> {}
//                }
//            }
//        }
    }

    fun addListener(listener: CallStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CallStateListener) {
        listeners.remove(listener)
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)

//        if (state == STATE_INITIALIZING) {
//            listeners.forEach {
//                it.onStateChanged(
//                    CallState.Registered(
//                        callAttributes = CallAttributesCompat(
//                            displayName = call.caller,
//                            address = call.callerNumber.toUri(),
//                            direction = CallAttributesCompat.DIRECTION_INCOMING
//                        ),
//                        isMuted = true,
//                        roomAlias = call.roomAlias,
//                        errorCode = null,
//                        isOnHold = false,
//                        isActive = false,
//                        callStartTimeMark = TimeSource.Monotonic.markNow(),
//                        actionSource = actionSource
//                    )
//                )
//            }
//        }

//        if (state == STATE_RINGING) {
//            listeners.forEach {
//                it.onStateChanged(
//                    CallState.Registered(
//                        callAttributes = CallAttributesCompat(
//                            displayName = call.caller,
//                            address = call.callerNumber.toUri(),
//                            direction = CallAttributesCompat.DIRECTION_INCOMING
//                        ),
//                        isMuted = true,
//                        roomAlias = call.roomAlias,
//                        errorCode = null,
//                        isOnHold = false,
//                        isActive = false,
//                        callStartTimeMark = TimeSource.Monotonic.markNow(),
//                        actionSource = actionSource
//                    )
//                )
//            }
//        }

//        if (state == STATE_ACTIVE) {
//            listeners.forEach {
//                it.onStateChanged(
//                    CallState.Registered(
//                        callAttributes = CallAttributesCompat(
//                            displayName = call.caller,
//                            address = call.callerNumber.toUri(),
//                            direction = CallAttributesCompat.DIRECTION_INCOMING
//                        ),
//                        isMuted = true,
//                        roomAlias = call.roomAlias,
//                        errorCode = null,
//                        isOnHold = false,
//                        isActive = true,
//                        callStartTimeMark = TimeSource.Monotonic.markNow(),
//                        actionSource = actionSource
//                    )
//                )
//            }
//        }

//        if (state == STATE_DISCONNECTED) {
//            listeners.forEach {
//                it.onStateChanged(
//                    CallState.Unregistered(
//                        callAttributes = CallAttributesCompat(
//                            displayName = call.caller,
//                            address = call.callerNumber.toUri(),
//                            direction = CallAttributesCompat.DIRECTION_INCOMING
//                        ),
//                        disconnectCause = disconnectCause,
//                        roomAlias = call.roomAlias
//                    )
//                )
//            }
//        }
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.d(TAG, "onDisconnect")
        closeConnection(DisconnectCause(DisconnectCause.LOCAL, "disconnected"))
    }

    override fun onAnswer() {
        setActive()
        listeners.forEach {
            it.onAnswer()
        }
    }

    override fun onReject() {
        Log.d(TAG, "onReject without params")
        closeConnection(DisconnectCause(DisconnectCause.REJECTED, "Rejected"))
    }

    override fun onAbort() {
        super.onAbort()
        Log.e(TAG, "OnAbort")
    }

    override fun onReject(rejectReason: Int) {
        super.onReject(rejectReason)
        closeConnection(DisconnectCause(DisconnectCause.REJECTED, rejectReason.toString()))
    }

    override fun onReject(replyMessage: String?) {
        super.onReject(replyMessage)
        closeConnection(DisconnectCause(DisconnectCause.REJECTED, replyMessage))
    }

    private fun closeConnection(cause: DisconnectCause) {
        setDisconnected(cause)
        destroy()
    }

    interface CallStateListener {

        fun onStateChanged(callState: CallState)

        fun onMuteStatusChanged(isMuted: Boolean)

        fun onAnswer()
    }
}