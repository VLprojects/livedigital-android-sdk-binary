package space.livedigital.example.calls.telecom.repositories

import android.telecom.DisconnectCause
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.telecom.entities.CallConnection

class TelecomCallRepository private constructor() : CallConnection.CallStateListener {

    val currentCallState
        get() = _currentCallState.asStateFlow()

    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.None)

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

    override fun onStateChanged(callState: CallState) {
        _currentCallState.value = callState
    }

    override fun onMuteStatusChanged() {
        val currentState = _currentCallState.value

        if (currentState is CallState.Registered) {
            _currentCallState.update {
                currentState.copy(isMuted = !currentState.isMuted)
            }
        }
    }

    override fun onAnswer() {}

    companion object {
        var instance: TelecomCallRepository? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create
         */
        fun create(): TelecomCallRepository {
            Log.d("MPB", "New instance")
            check(instance == null) {
                "CallRepository instance already created"
            }

            return TelecomCallRepository().also {
                instance = it
            }
        }
    }
}