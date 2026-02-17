package space.livedigital.example.calls.telecom.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.telecom.entities.CallConnection
import space.livedigital.example.calls.telecom.entities.CallFromPush

class TelecomCallRepository private constructor() : CallConnection.CallStateListener {

    val currentCallState
        get() = _currentCallState.asStateFlow()

    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.None)

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

    interface CallObserver {

        fun onCallEnded(call: CallFromPush)
    }

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