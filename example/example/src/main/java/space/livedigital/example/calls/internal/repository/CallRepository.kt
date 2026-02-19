package space.livedigital.example.calls.internal.repository

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState

// In real app we need to put all android related logic in service
class CallRepository private constructor(private val callsManager: CallsManager) {

    val currentCallState
        get() = _currentCallState.asStateFlow()
    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.None)

    suspend fun registerCall(
        displayName: String,
        roomAlias: String,
        phoneNumber: Uri,
        isIncoming: Boolean
    ) {
        val actionSource = Channel<CallAction>()

        val callAttributes = CallAttributesCompat(
            displayName = displayName,
            address = phoneNumber,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            }
        )

        try {
            callsManager.addCall(
                callAttributes,
                onIsCallAnswered,
                onIsCallDisconnected,
                onIsCallActive,
                onIsCallInactive
            ) {
                launch {
                    processCallActions(actionSource.consumeAsFlow())
                }

                _currentCallState.value = CallState.Registered(
                    callAttributes = callAttributes,
                    isMuted = true,
                    roomAlias = roomAlias,
                    errorCode = null,
                    isActive = false,
                    isOnHold = false,
                    actionSource = actionSource,
                )

                if (!isIncoming) {
                    launch {
                        actionSource.send(CallAction.Activate)
                    }
                }
            }
        } finally {
            _currentCallState.value = CallState.None
        }
    }

    fun endCall() {
        val call = _currentCallState.value
        if (call is CallState.Registered) {
            if (call.isActive) {
                call.actionSource.trySend(
                    CallAction.Disconnect(DisconnectCause(DisconnectCause.REMOTE))
                )
            } else {
                call.actionSource.trySend(
                    CallAction.Disconnect(DisconnectCause(DisconnectCause.MISSED))
                )
            }
        }
    }

    /**
     * Collect the action source to handle client actions inside the call scope
     */
    private suspend fun CallControlScope.processCallActions(actionSource: Flow<CallAction>) {
        actionSource.collect { action ->
            when (action) {
                CallAction.Answer -> {
                    doAnswer()
                }

                is CallAction.Disconnect -> {
                    doDisconnect(action)
                }

                CallAction.Activate -> {
                    when (val result = setActive()) {
                        is CallControlResult.Success -> {
                            onIsCallActive()
                        }

                        is CallControlResult.Error -> {
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                is CallAction.ToggleMute -> {
                    updateCurrentCall {
                        copy(isMuted = !isMuted)
                    }
                }
            }
        }
    }

    private suspend fun CallControlScope.doAnswer() {
        when (answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
            is CallControlResult.Success -> {
                onIsCallAnswered(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            }

            is CallControlResult.Error -> {
                updateCurrentCall {
                    CallState.Unregistered(
                        callAttributes = callAttributes,
                        disconnectCause = DisconnectCause(DisconnectCause.BUSY),
                        roomAlias = roomAlias
                    )
                }
            }
        }
    }

    private suspend fun CallControlScope.doDisconnect(action: CallAction.Disconnect) {
        disconnect(action.cause)
        onIsCallDisconnected(action.cause)
    }


    /**
     *  Can the call be successfully answered??
     *  TIP: We would check the connection/call state to see if we can answer a call
     *  Example you may need to wait for another call to hold.
     **/
    val onIsCallAnswered: suspend (type: Int) -> Unit = {
        updateCurrentCall {
            copy(isActive = true, isOnHold = false)
        }
    }

    /**
     * Can the call perform a disconnect
     */
    val onIsCallDisconnected: suspend (cause: DisconnectCause) -> Unit = {
        updateCurrentCall {
            CallState.Unregistered(callAttributes, it, roomAlias)
        }
    }

    /**
     *  Check is see if we can make the call active.
     *  Other calls and state might stop us from activating the call
     */
    val onIsCallActive: suspend () -> Unit = {
        Log.d("xd", "onIsCallActive")
        updateCurrentCall {
            copy(
                errorCode = null,
                isActive = true,
                isOnHold = false,
            )
        }
    }

    /**
     * Check to see if we can make the call inactivate
     */
    val onIsCallInactive: suspend () -> Unit = {
        updateCurrentCall {
            copy(
                errorCode = null,
                isOnHold = true
            )
        }
    }

    interface CallObserver {

        fun onCallEnded(callAttributes: CallAttributesCompat)
    }

    private fun updateCurrentCall(transform: CallState.Registered.() -> CallState) {
        _currentCallState.update { call ->
            if (call is CallState.Registered) {
                call.transform()
            } else {
                call
            }
        }
    }

    companion object {
        var instance: CallRepository? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create a singleton with the CallsManager class.
         */
        fun create(context: Context): CallRepository {
            Log.d("MPB", "New instance")
            check(instance == null) {
                "CallRepository instance already created"
            }

            val callsManager = CallsManager(context).apply {
                registerAppWithTelecom(
                    capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
                )
            }

            return CallRepository(
                callsManager = callsManager,
            ).also {
                instance = it
            }
        }
    }
}