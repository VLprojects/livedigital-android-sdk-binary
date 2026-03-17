package space.livedigital.example.calls.telecom.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
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
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.entities.CallConnection
import space.livedigital.example.calls.telecom.entities.EmptyConnection

class CallConnectionService : ConnectionService() {

    private val listener = object : CallConnection.CallStateListener {
        override fun onAnswer(call: Call) {
            repository?.dispatchCallAction(
                CallAction.Activate(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias
                )
            )
            val intent = Intent(this@CallConnectionService, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
                    cause = disconnectCause
                )
            )
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
        val bundle = request?.extras ?: return EmptyConnection
        val caller = bundle.getString(CallConstants.EXTRA_NAME) ?: return EmptyConnection
        val roomAlias = bundle.getString(CallConstants.EXTRA_ROOM_ALIAS) ?: return EmptyConnection
        val call = Call.Actual(
            displayName = caller,
            phone = request.address.schemeSpecificPart,
            roomAlias = roomAlias,
        )
        val connection = CallConnection(call).apply {
            connectionProperties = Connection.PROPERTY_SELF_MANAGED
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
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
        val call = Call.Actual(
            displayName = name,
            phone = request.address.schemeSpecificPart,
            roomAlias = roomAlias,
        )

        val connection = CallConnection(call).apply {
            connectionProperties = Connection.PROPERTY_SELF_MANAGED
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
            addListener(listener)
            setRinging()
        }

        this.connection = connection
        return connection
    }

    private fun updateConnectionState(callState: CallState) {
        when (callState) {
            is CallState.Active -> {
                connection?.setActive()
            }

            is CallState.Ended -> {
                connection?.setDisconnected(callState.disconnectCause)
                connection?.destroy()
            }

            is CallState.Missed -> {
                connection?.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
                connection?.destroy()
            }

            is CallState.Answered -> {
                repository?.dispatchCallAction(
                    CallAction.Activate(
                        displayName = callState.call.displayName,
                        phone = callState.call.phone,
                        roomAlias = callState.call.roomAlias
                    )
                )
                val intent = Intent(this@CallConnectionService, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

            else -> Unit
        }
    }
}