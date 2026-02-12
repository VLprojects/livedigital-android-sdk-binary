package space.livedigital.example.telecom_calls.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri

class CallHandler {

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun startIncomingCall(context: Context, call: Call): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = telecomManager.callCapablePhoneAccounts.firstOrNull {
            it.componentName == ComponentName(context, CallConnectionService::class.java)
        }

        if (phoneAccountHandle == null) return false

        val extras = Bundle()

        extras.putString("id", call.id)
        extras.putString("roomAlias", call.roomAlias)
        extras.putString("caller", call.caller)

        extras.putParcelable(
            TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
            Uri.fromParts(
                PhoneAccount.SCHEME_TEL,
                call.callerNumber,   // must be digits or +E.164
                null
            )
        )

        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)

        try {
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
            return true
        } catch (e: SecurityException) {
            Log.d(TAG, "Permission not granted. e = $e")
            return false
        } catch (e: Exception) {
            Log.d(TAG, "exception = $e")
            return false
        }
    }

    companion object {
        private const val TAG = "CallHandler"
    }
}