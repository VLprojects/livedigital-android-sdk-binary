package space.livedigital.example.telecom_calls.utils

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.DisconnectCause

class CallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val bundle = request?.extras ?: return EmptyConnection
        val caller = bundle.getString("caller") ?: return EmptyConnection
        val roomAlias = bundle.getString("roomAlias") ?: return EmptyConnection
        val id = bundle.getString("id") ?: return EmptyConnection
        val call =
            Call(id = id, caller = caller, roomAlias = roomAlias, callerNumber = "callerNumber")
        val connection = CallConnection(applicationContext, call)
        connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        connection.setCallerDisplayName(caller, TelecomManager.PRESENTATION_ALLOWED)
        connection.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        TelecomCallRepository.initializeConnection(connection)
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
