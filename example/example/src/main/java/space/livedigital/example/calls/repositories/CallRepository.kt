package space.livedigital.example.calls.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import kotlin.time.TimeSource

internal class CallRepository private constructor() {

    val currentCallState
        get() = _currentCallState.asStateFlow()
    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.Idle)

    fun dispatchCallAction(callAction: CallAction) {
        _currentCallState.update { callState ->
            when (callAction) {
                is CallAction.Activate -> {
                    val wasMuted = (callState as? CallState.Outgoing)?.isMuted
                    CallState.Activated(
                        call = Call.Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias
                        ),
                        isMuted = wasMuted ?: true
                    )
                }

                is CallAction.Answer -> {
                    CallState.Answered(
                        call = Call.Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias
                        ),
                        isMuted = true
                    )
                }

                is CallAction.PlaceActiveCall -> {
                    val wasMuted = (callState as? CallState.Activated)?.isMuted

                    CallState.Active(
                        call = Call.Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias
                        ),
                        isMuted = wasMuted ?: true,
                        startTimeMark = TimeSource.Monotonic.markNow()
                    )
                }

                is CallAction.Disconnect -> {
                    if (callState is CallState.Active ||
                        callState is CallState.Answered ||
                        callState is CallState.Outgoing
                    ) {
                        CallState.Ended(
                            call = Call.Actual(
                                displayName = callAction.displayName,
                                phone = callAction.phone,
                                roomAlias = callAction.roomAlias
                            ),
                            wasActive = callState !is CallState.Outgoing,
                            disconnectCause = callAction.cause
                        )
                    } else {
                        CallState.Missed(
                            call = Call.Actual(
                                displayName = callAction.displayName,
                                phone = callAction.phone,
                                roomAlias = callAction.roomAlias
                            ),
                            disconnectCause = callAction.cause
                        )
                    }
                }

                is CallAction.PlaceIncomingCall -> {
                    CallState.Incoming(
                        call = Call.Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias
                        )
                    )
                }

                is CallAction.PlaceOutgoingCall -> {
                    CallState.Outgoing(
                        call = Call.Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                        ),
                        isMuted = true
                    )
                }

                is CallAction.ToggleMute -> {
                    if (callState is CallState.Active) {
                        return@update callState.copy(isMuted = callAction.isMute)
                    }

                    if (callState is CallState.Answered) {
                        return@update callState.copy(isMuted = callAction.isMute)
                    }

                    if (callState is CallState.Outgoing) {
                        return@update callState.copy(isMuted = callAction.isMute)
                    }

                    callState
                }
            }
        }
    }

    companion object {
        var instance: CallRepository? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create a singleton.
         */
        fun create(): CallRepository {
            Log.d("MPB", "New instance")
            check(instance == null) {
                "CallRepository instance already created"
            }

            return CallRepository().also {
                instance = it
            }
        }
    }
}