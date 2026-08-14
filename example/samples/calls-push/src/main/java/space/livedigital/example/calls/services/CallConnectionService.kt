package space.livedigital.example.calls.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.OutcomeReceiver
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallActivityAction
import space.livedigital.example.calls.entities.CallCommand
import space.livedigital.example.calls.entities.CallConnection
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.utils.CallHandler
import space.livedigital.example.calls.utils.initialIsMuted

class CallConnectionService : ConnectionService() {

    private val listener = object : CallConnection.CallStateListener {
        override fun onAnswer(call: Call) {
            repository?.dispatchCallAction(
                CallAction.Answer(
                    call = call,
                    isMuted = applicationContext.initialIsMuted()
                )
            )
            launchCallActivity()
        }

        override fun onDisconnect(
            call: Call,
            disconnectCause: DisconnectCause
        ) {
            repository?.dispatchCallAction(
                CallAction.Disconnect(call = call, cause = disconnectCause)
            )
        }

        override fun onCallEndpointChanged(callEndpoint: GeneralCallEndpoint) {
            repository?.onCallCurrentEndpointChanged(callEndpoint)
        }

        override fun onCallEndpointsChanged(callEndpoints: List<GeneralCallEndpoint>) {
            repository?.onCallEndpointsChanged(callEndpoints)
        }
    }

    private var scope: CoroutineScope? = null
    private var repository: CallRepository? = null
    private var connection: Connection? = null

    override fun onCreate() {
        super.onCreate()
        repository = CallRepository.instance ?: CallRepository.create()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope?.let {
            repository?.currentCallState
                ?.onEach { callState ->
                    updateConnectionState(callState)
                }
                ?.onCompletion {
                    stopSelf()
                }
                ?.launchIn(it)
            repository?.commands
                ?.onEach { command ->
                    when (command) {
                        is CallCommand.ChangeEndpoint -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                connection?.requestCallEndpointChange(
                                    command.endpoint.rawEndpoint as CallEndpoint,
                                    Runnable::run,
                                    object : OutcomeReceiver<Void?, CallEndpointException> {
                                        override fun onResult(result: Void?) {}
                                        override fun onError(error: CallEndpointException) {}
                                    })
                            } else {
                                connection?.setAudioRoute(command.endpoint.rawEndpoint as Int)
                            }
                        }
                    }
                }
                ?.launchIn(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (connection as? CallConnection)?.removeListener(listener)
        connection = null
        repository = null
        scope?.cancel()
        scope = null
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val bundle = request?.extras ?: return Connection.createFailedConnection(
            DisconnectCause(DisconnectCause.RESTRICTED)
        )
        val caller =
            bundle.getString(CallConstants.EXTRA_NAME) ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.RESTRICTED)
            )
        val signalingToken = bundle.getString(CallConstants.EXTRA_SIGNALING_TOKEN)
            ?: return Connection.createFailedConnection(DisconnectCause(DisconnectCause.RESTRICTED))
        val call = Call.Actual(
            displayName = caller,
            phone = request.address.schemeSpecificPart,
            signalingToken = signalingToken
        )
        val connection = CallConnection(call).apply {
            audioModeIsVoip = true
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
            repository?.dispatchCallAction(CallAction.PlaceIncomingCall(call = call))
        }

        this.connection = connection
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val address = request?.address ?: return Connection.createFailedConnection(
            DisconnectCause(DisconnectCause.RESTRICTED)
        )
        val bundle = request.extras
        val signalingToken = bundle?.getString(CallConstants.EXTRA_SIGNALING_TOKEN)

        // A signaling token in the extras means this app placed the call itself (see
        // CallHandler.tryToStartSystemOutgoingCall) and already minted one. Otherwise the call
        // was placed some other way — e.g. redialed from the system call log, or dialed via the
        // native Phone app while this app's account is the default outgoing account — so there
        // is no pre-built Call to read; mint one from just the destination address instead.
        val call = if (signalingToken != null) {
            val name = bundle.getString(CallConstants.EXTRA_NAME)
                ?: return Connection.createFailedConnection(
                    DisconnectCause(DisconnectCause.RESTRICTED)
                )
            Call.Actual(
                displayName = name,
                phone = address.schemeSpecificPart,
                signalingToken = signalingToken
            )
        } else {
            CallHandler(applicationContext).buildOutgoingCall(address.schemeSpecificPart)
                ?: return Connection.createFailedConnection(DisconnectCause(DisconnectCause.RESTRICTED))
        }

        val connection = CallConnection(call).apply {
            audioModeIsVoip = true
            setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(call.displayName, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
            repository?.dispatchCallAction(
                CallAction.PlaceOutgoingCall(
                    call = call,
                    isMuted = applicationContext.initialIsMuted()
                )
            )
        }

        this.connection = connection
        return connection
    }

    private fun updateConnectionState(callState: CallState) {
        when (callState) {
            is CallState.Answered -> {
                connection?.setActive()
                launchCallActivity()
                repository?.dispatchCallAction(
                    CallAction.PlaceActiveCall(call = callState.call)
                )
            }

            is CallState.Activated -> {
                connection?.setActive()
                launchCallActivity()
                repository?.dispatchCallAction(
                    CallAction.PlaceActiveCall(call = callState.call)
                )
            }

            // The room join and engine.initiateCall() live in CallViewModel, which only exists
            // while CallActivity is shown. Bring it up over the system dialer right away —
            // otherwise the outbound call would sit ringing forever without dialing the callee.
            is CallState.Outgoing -> {
                launchCallActivity()
            }

            is CallState.Ended -> {
                stopService(Intent(applicationContext, CallConnectionAudioService::class.java))
                connection?.setDisconnected(callState.disconnectCause)
                connection?.destroy()
            }

            is CallState.Missed -> {
                stopService(Intent(applicationContext, CallConnectionAudioService::class.java))
                connection?.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
                connection?.destroy()
            }

            else -> Unit
        }
    }

    private fun launchCallActivity() {
        val intent = Intent(this@CallConnectionService, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(
                CallConstants.EXTRA_ACTION,
                CallActivityAction.StartBackgroundAudioService
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            this@CallConnectionService,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // We need to add a delay to prevent our app from being overlaid by the system dialer
        Handler(Looper.getMainLooper()).postDelayed({
            pendingIntent.send()
        }, 500)
    }
}