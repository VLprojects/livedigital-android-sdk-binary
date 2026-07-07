package space.livedigital.example.calls.repositories

import android.telecom.DisconnectCause
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import space.livedigital.example.BuildConfig
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.AudioState
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
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.sdk.Failure
import space.livedigital.sdk.Success
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.LiveDigitalEngineDestroyDelegate
import kotlin.time.TimeSource

class CallRepository private constructor() : KoinComponent {

    val currentCallState
        get() = _currentCallState.asStateFlow()
    val audioState
        get() = _audioState.asStateFlow()
    val commands
        get() = _commands.receiveAsFlow()
    private val _currentCallState: MutableStateFlow<CallState> = MutableStateFlow(CallState.Idle)
    private val _audioState = MutableStateFlow(AudioState())
    private val _commands = Channel<CallCommand>(Channel.BUFFERED)

    // Repository is a process-wide singleton, so the scope intentionally has no lifecycle owner:
    // a decline fired from a notification must survive the activity being closed. Main dispatcher
    // because the engine binds its internal thread checkers to the thread it was created on,
    // and destroy() disposes them on the main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


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
        val previousCallState = _currentCallState.value

        if (callAction is CallAction.Disconnect &&
            previousCallState is Incoming &&
            callAction.cause.isUserDecline()
        ) {
            declineSipCall(callAction.call.signalingToken)
        }

        _currentCallState.update { callState ->
            val previousMedia = callState as? CallState.WithMedia

            when (callAction) {
                is CallAction.Activate -> Activated(
                    call = callAction.call,
                    isMuted = previousMedia?.isMuted ?: false
                )

                is CallAction.Answer -> Answered(
                    call = callAction.call,
                    isMuted = callAction.isMuted
                )

                is CallAction.PlaceActiveCall -> Active(
                    call = callAction.call,
                    isMuted = previousMedia?.isMuted ?: false,
                    startTimeMark = TimeSource.Monotonic.markNow()
                )

                is CallAction.Disconnect -> {
                    if (callState is Active ||
                        callState is Answered ||
                        callState is Outgoing ||
                        callAction.cause.code == DisconnectCause.ANSWERED_ELSEWHERE
                    ) {
                        Ended(
                            call = callAction.call,
                            wasActive = callState !is Outgoing,
                            disconnectCause = callAction.cause
                        )
                    } else {
                        Missed(
                            call = callAction.call,
                            disconnectCause = callAction.cause
                        )
                    }
                }

                is CallAction.PlaceIncomingCall -> Incoming(call = callAction.call)

                is CallAction.PlaceOutgoingCall -> Outgoing(
                    call = callAction.call,
                    isMuted = callAction.isMuted
                )

                is CallAction.ToggleMute ->
                    previousMedia?.copyMedia(isMuted = callAction.isMute) ?: callState

                is CallAction.PlaceMissedCall -> Missed(
                    call = callAction.call,
                    disconnectCause = DisconnectCause(DisconnectCause.MISSED)
                )
            }
        }
    }

    /**
     * A decline made by the user on this device: the in-app reject button dispatches LOCAL,
     * the system dialer and the call-style notification dispatch REJECTED. A REJECTED marked
     * with [CallConstants.REASON_DECLINED_ELSEWHERE] comes from the `call_declined_by_callee`
     * push (declined on another device) — the backend already knows, don't echo it back.
     */
    private fun DisconnectCause.isUserDecline(): Boolean {
        val isDeclineCode = code == DisconnectCause.LOCAL || code == DisconnectCause.REJECTED
        return isDeclineCode && reason != CallConstants.REASON_DECLINED_ELSEWHERE
    }

    private fun declineSipCall(signalingToken: String) {
        scope.launch {
            val engine = get<LiveDigitalEngine> {
                parametersOf(
                    BuildConfig.LOAD_BALANCER_BASE_URL,
                    BuildConfig.SIGNALING_API_BASE_URL
                )
            }
            when (val result = engine.declineCall(signalingToken)) {
                is Success -> Log.d(TAG, "SIP call declined")
                is Failure -> Log.e(TAG, "Failed to decline SIP call: ${result.error}")
            }
            engine.destroy(
                object : LiveDigitalEngineDestroyDelegate {
                    override fun onDestroyed() = Unit
                }
            )
        }
    }

    companion object {
        private const val TAG = "CallRepository"

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