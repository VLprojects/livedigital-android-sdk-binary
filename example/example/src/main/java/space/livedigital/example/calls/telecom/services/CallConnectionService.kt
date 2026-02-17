package space.livedigital.example.calls.telecom.services

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
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.telecom.entities.CallConnection
import space.livedigital.example.calls.telecom.entities.CallFromPush
import space.livedigital.example.calls.telecom.entities.EmptyConnection
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

class CallConnectionService : ConnectionService() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private val listener by lazy {
        object : CallConnection.CallStateListener {
            override fun onStateChanged(callState: CallState) {}

            override fun onMuteStatusChanged() {}

            override fun onAnswer() {
                val intent = Intent(this@CallConnectionService, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(
                        CallConstants.EXTRA_ACTION,
                        CallAction.Answer,
                    )
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(intent)
                }, 500)
            }
        }
    }
    private var repository: TelecomCallRepository? = null
    private var connection: Connection? = null

    override fun onCreate() {
        super.onCreate()
        repository = TelecomCallRepository.instance ?: TelecomCallRepository.create()
    }

    override fun onDestroy() {
        super.onDestroy()
        (connection as? CallConnection)?.apply {
            removeListener(repository as CallConnection.CallStateListener)
            removeListener(listener)
        }
        connection = null
        repository = null
        scope.cancel()
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
        val connection = CallConnection(scope, call).apply {
            connectionProperties = Connection.PROPERTY_SELF_MANAGED
            setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
            addListener(repository as CallConnection.CallStateListener)
            addListener(listener)
            setInitializing()
        }

        this.connection = connection
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        return Connection.createFailedConnection(
            DisconnectCause(DisconnectCause.RESTRICTED)
        )
    }
}