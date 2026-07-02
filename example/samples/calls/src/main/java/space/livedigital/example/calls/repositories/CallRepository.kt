package space.livedigital.example.calls.repositories

import android.telecom.DisconnectCause
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import space.livedigital.example.calls.entities.AudioState
import space.livedigital.example.calls.entities.Call.Actual
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallCommand
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.CallState.Activated
import space.livedigital.example.calls.entities.CallState.Active
import space.livedigital.example.calls.entities.CallState.Answered
import space.livedigital.example.calls.entities.CallState.Ended
import space.livedigital.example.calls.entities.CallState.Incoming
import space.livedigital.example.calls.entities.CallState.Missed
import space.livedigital.example.calls.entities.CallState.Outgoing
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import kotlin.time.TimeSource

class CallRepository private constructor() {

    val currentCallState
        get() = _currentCallState.asStateFlow()
    val audioState
        get() = _audioState.asStateFlow()
    val commands
        get() = _commands.receiveAsFlow()
    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.Idle)
    private val _audioState = MutableStateFlow(AudioState())
    private val _commands = Channel<CallCommand>(Channel.BUFFERED)


    suspend fun dispatchCallCommand(callCommand: CallCommand) {
        _commands.send(callCommand)
    }

    fun onCallEndpointsChanged(callEndpoints: List<GeneralCallEndpoint>) {
        val sortedList = callEndpoints.sortedBy {
            it.type.rank
        }
        _audioState.update {
            it.copy(availableEndpoints = sortedList)
        }
    }

    fun onCallCurrentEndpointChanged(currentCallEndpoint: GeneralCallEndpoint) {
        _audioState.update {
            it.copy(currentEndpoint = currentCallEndpoint)
        }
    }

    fun dispatchCallAction(callAction: CallAction) {
        _currentCallState.update { callState ->
            when (callAction) {
                is CallAction.Activate -> {
                    val wasMuted = (callState as? CallState.Outgoing)?.isMuted
                    val wasCameraOn = (callState as? CallState.Outgoing)?.isCameraOn
                    Activated(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        ),
                        isMuted = wasMuted ?: false,
                        isCameraOn = wasCameraOn ?: (callAction.callType == CallType.VIDEO)
                    )
                }

                is CallAction.Answer -> {
                    Answered(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        ),
                        isMuted = callAction.isMuted,
                        isCameraOn = callAction.isCameraOn,
                    )
                }

                is CallAction.PlaceActiveCall -> {
                    val wasMuted = (callState as? CallState.Activated)?.isMuted
                        ?: (callState as? CallState.Answered)?.isMuted
                    val wasCameraOn = (callState as? CallState.Activated)?.isCameraOn
                        ?: (callState as? CallState.Answered)?.isCameraOn

                    Active(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        ),
                        isMuted = wasMuted ?: false,
                        isCameraOn = wasCameraOn ?: (callAction.callType == CallType.VIDEO),
                        startTimeMark = TimeSource.Monotonic.markNow()
                    )
                }

                is CallAction.Disconnect -> {
                    if (callState is CallState.Active ||
                        callState is CallState.Answered ||
                        callState is CallState.Outgoing
                    ) {
                        Ended(
                            call = Actual(
                                displayName = callAction.displayName,
                                phone = callAction.phone,
                                roomAlias = callAction.roomAlias,
                                callType = callAction.callType
                            ),
                            wasActive = callState !is CallState.Outgoing,
                            disconnectCause = callAction.cause
                        )
                    } else {
                        Missed(
                            call = Actual(
                                displayName = callAction.displayName,
                                phone = callAction.phone,
                                roomAlias = callAction.roomAlias,
                                callType = callAction.callType
                            ),
                            disconnectCause = callAction.cause
                        )
                    }
                }

                is CallAction.PlaceIncomingCall -> {
                    Incoming(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        )
                    )
                }

                is CallAction.PlaceOutgoingCall -> {
                    Outgoing(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        ),
                        isMuted = callAction.isMuted,
                        isCameraOn = callAction.isCameraOn
                    )
                }

                is CallAction.ToggleMute -> {
                    if (callState is CallState.Active) {
                        return@update callState.copy(isMuted = callAction.isMute)
                    }

                    if (callState is CallState.Activated) {
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

                is CallAction.ToggleCamera -> {
                    if (callState is CallState.Active) {
                        return@update callState.copy(isCameraOn = callAction.isCameraOn)
                    }

                    if (callState is CallState.Activated) {
                        return@update callState.copy(isCameraOn = callAction.isCameraOn)
                    }

                    if (callState is CallState.Answered) {
                        return@update callState.copy(isCameraOn = callAction.isCameraOn)
                    }

                    if (callState is CallState.Outgoing) {
                        return@update callState.copy(isCameraOn = callAction.isCameraOn)
                    }

                    callState
                }

                is CallAction.PlaceMissedCall -> {
                    Missed(
                        call = Actual(
                            displayName = callAction.displayName,
                            phone = callAction.phone,
                            roomAlias = callAction.roomAlias,
                            callType = callAction.callType
                        ),
                        disconnectCause = DisconnectCause(DisconnectCause.MISSED)
                    )
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