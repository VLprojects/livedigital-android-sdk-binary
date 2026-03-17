package space.livedigital.example.calls.telecom.services

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.entities.CallConnection
import space.livedigital.example.calls.telecom.entities.CallFromPush
import space.livedigital.example.calls.telecom.entities.EmptyConnection

class CallConnectionService : ConnectionService() {

    private var scope: CoroutineScope? = null

    private val listener by lazy {
        object : CallConnection.CallStateListener {
            override fun onStateChanged(callState: CallState) {
            }

            override fun onMuteStatusChanged(isMuted: Boolean) {}

            override fun onAnswer() {
                val intent = Intent(this@CallConnectionService, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    putExtra(
//                        CallConstants.EXTRA_ACTION,
//                        CallAction.Answer,
//                    )
                }
                // We need to add a delay to prevent our app from being overlaid by the system
                // dialer
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(intent)
                }, 500)
            }
        }
    }
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
        (connection as? CallConnection)?.apply {
            removeListener(repository as CallConnection.CallStateListener)
            removeListener(listener)
        }
        connection = null
        repository = null
        scope?.cancel()
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val bundle = request?.extras ?: return EmptyConnection
        val id = bundle.getString(CallConstants.EXTRA_ID) ?: return EmptyConnection
        val caller = bundle.getString(CallConstants.EXTRA_NAME) ?: return EmptyConnection
        val roomAlias = bundle.getString(CallConstants.EXTRA_ROOM_ALIAS) ?: return EmptyConnection
        val call = CallFromPush(
            id = id,
            caller = caller,
            roomAlias = roomAlias,
            callerNumber = request.address.schemeSpecificPart
        )
//        val connection = CallConnection(scope, call).apply {
//            connectionProperties = Connection.PROPERTY_SELF_MANAGED
//            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
//            setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
//            addListener(repository as CallConnection.CallStateListener)
//            addListener(listener)
//            setRinging()
//        }

//        this.connection = connection
        return EmptyConnection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        return Connection.createFailedConnection(
            DisconnectCause(DisconnectCause.RESTRICTED)
        )
    }

    private fun updateConnectionState(callState: CallState) {
//        when (callState) {
//            is CallState.Active ->
//            is CallState.Ended -> TODO()
//            CallState.Idle -> connection?.destroy()
//            is CallState.Incoming -> TODO()
//            is CallState.Missed -> TODO()
//            is CallState.Outgoing -> TODO()
//        }
    }
}