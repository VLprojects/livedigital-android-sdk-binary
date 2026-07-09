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
import android.telecom.VideoProfile
import kotlinx.coroutines.CoroutineScope
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
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.utils.initialIsCameraOn
import space.livedigital.example.calls.utils.initialIsMuted

class CallConnectionService : ConnectionService() {

    private val listener = object : CallConnection.CallStateListener {
        override fun onAnswer(call: Call) {
            repository?.dispatchCallAction(
                CallAction.Answer(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType,
                    isMuted = applicationContext.initialIsMuted(),
                    isCameraOn = applicationContext.initialIsCameraOn(call.callType)
                )
            )
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
            // We need to add a delay to prevent our app from being overlaid by the system
            // dialer
            Handler(Looper.getMainLooper()).postDelayed({
                pendingIntent.send()
            }, 500)
        }

        override fun onDisconnect(
            call: Call,
            disconnectCause: DisconnectCause
        ) {
            repository?.dispatchCallAction(
                CallAction.Disconnect(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType,
                    cause = disconnectCause
                )
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
        scope = CoroutineScope(SupervisorJob())
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
        val roomAlias = bundle.getString(CallConstants.EXTRA_ROOM_ALIAS)
            ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.RESTRICTED)
            )
        val callType = CallType.valueOf(
            bundle.getString(CallConstants.EXTRA_CALL_TYPE)?.uppercase()
                ?: return Connection.createFailedConnection(
                    DisconnectCause(
                        DisconnectCause.RESTRICTED
                    )
                )
        )
        val call = Call.Actual(
            displayName = caller,
            phone = request.address.schemeSpecificPart,
            roomAlias = roomAlias,
            callType = callType
        )
        val connection = CallConnection(call).apply {
            audioModeIsVoip = true
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)

            if (callType == CallType.VIDEO) {
                connectionCapabilities = Connection.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or
                        Connection.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL
                videoState = VideoProfile.STATE_BIDIRECTIONAL
            }

            setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
            repository?.dispatchCallAction(
                CallAction.PlaceIncomingCall(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType
                )
            )
        }

        this.connection = connection
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val bundle = request?.extras ?: return Connection.createFailedConnection(
            DisconnectCause(DisconnectCause.RESTRICTED)
        )
        val roomAlias = bundle.getString(CallConstants.EXTRA_ROOM_ALIAS)
            ?: return Connection.createFailedConnection(DisconnectCause(DisconnectCause.RESTRICTED))
        val name =
            bundle.getString(CallConstants.EXTRA_NAME) ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.RESTRICTED)
            )
        val callType = CallType.valueOf(
            bundle.getString(CallConstants.EXTRA_CALL_TYPE)?.uppercase()
                ?: return Connection.createFailedConnection(
                    DisconnectCause(
                        DisconnectCause.RESTRICTED
                    )
                )
        )
        val call = Call.Actual(
            displayName = name,
            phone = request.address.schemeSpecificPart,
            roomAlias = roomAlias,
            callType = callType
        )

        val connection = CallConnection(call).apply {
            audioModeIsVoip = true
            if (callType == CallType.VIDEO) {
                connectionCapabilities = Connection.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or
                        Connection.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL
                videoState = VideoProfile.STATE_BIDIRECTIONAL
            }
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
            repository?.dispatchCallAction(
                CallAction.PlaceOutgoingCall(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType,
                    isMuted = applicationContext.initialIsMuted(),
                    isCameraOn = applicationContext.initialIsCameraOn(call.callType)
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

                // We need to add a delay to prevent our app from being overlaid by the system
                // dialer
                Handler(Looper.getMainLooper()).postDelayed({
                    pendingIntent.send()
                }, 500)
                repository?.dispatchCallAction(
                    CallAction.PlaceActiveCall(
                        displayName = callState.call.displayName,
                        phone = callState.call.phone,
                        roomAlias = callState.call.roomAlias,
                        callType = callState.call.callType
                    )
                )
            }

            is CallState.Activated -> {
                connection?.setActive()
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

                // We need to add a delay to prevent our app from being overlaid by the system
                // dialer
                Handler(Looper.getMainLooper()).postDelayed({
                    pendingIntent.send()
                }, 500)
                repository?.dispatchCallAction(
                    CallAction.PlaceActiveCall(
                        displayName = callState.call.displayName,
                        phone = callState.call.phone,
                        roomAlias = callState.call.roomAlias,
                        callType = callState.call.callType
                    )
                )
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
}