package space.livedigital.example.calls.telecom

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.ActivityCompat
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.broadcasts.CallBroadcast
import space.livedigital.example.calls.telecom.entities.CallFromPush
import space.livedigital.example.calls.telecom.services.CallConnectionService

class CallHandler(
    private val context: Context
) {

    fun startIncomingCall(call: CallFromPush) {
        val hasReadPhoneStatePermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        // Start self-managed call if we cannon check phone accounts
        if (!hasReadPhoneStatePermission) {
            context.launchCall(
                name = call.caller,
                phoneNumber = call.callerNumber,
                roomAlias = call.roomAlias
            )
            return
        }

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = telecomManager.callCapablePhoneAccounts.firstOrNull {
            it.componentName == ComponentName(context, CallConnectionService::class.java)
        }

        // Start self-managed call if our phone account not registered
        if (phoneAccountHandle == null) {
            context.launchCall(
                name = call.caller,
                phoneNumber = call.callerNumber,
                roomAlias = call.roomAlias
            )
            return
        }

        val extras = Bundle().apply {
            putString(CallConstants.EXTRA_ID, call.id)
            putString(CallConstants.EXTRA_ROOM_ALIAS, call.roomAlias)
            putString(CallConstants.EXTRA_NAME, call.caller)
            putParcelable(
                TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                Uri.fromParts(
                    PhoneAccount.SCHEME_TEL,
                    call.callerNumber,   // must be digits or +E.164
                    null
                )
            )
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        }

        try {
            // Start telecom call
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
        } catch (e: SecurityException) {
            Log.d(TAG, "Permission not granted. e = $e")
            // If there no permission to start telecom call, we should start self-managed call
            context.launchCall(
                name = call.caller,
                phoneNumber = call.callerNumber,
                roomAlias = call.roomAlias
            )
            return
        } catch (e: Exception) {
            Log.d(TAG, "exception = $e")
            return
        }
    }

    // Start self-managed call
    private fun Context.launchCall(
        name: String,
        phoneNumber: String,
        roomAlias: String
    ) {
        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
        callIntent.putExtra(
            CallConstants.EXTRA_ACTION,
            CallAction.PlaceIncomingCall(
                displayName = name,
                phone = phoneNumber,
                roomAlias = roomAlias
            ),
        )
        sendBroadcast(callIntent)
    }

    companion object {
        private const val TAG = "CallHandler"
    }
}