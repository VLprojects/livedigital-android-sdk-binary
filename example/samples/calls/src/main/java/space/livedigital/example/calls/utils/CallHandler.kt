package space.livedigital.example.calls.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.ActivityCompat
import space.livedigital.example.calls.broadcasts.CallBroadcast
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.services.CallConnectionService

class CallHandler(
    private val context: Context
) {

    fun startIncomingCall(call: Call) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = tryToGetPhoneAccountHandle(telecomManager)

        // Mi UI has custom permission which disallowed to start activity from background
        val isMiUI = XiaomiUtilities.isMIUI
        val isPermissionToStartActivityFromBackgroundGranted =
            XiaomiUtilities.isCustomPermissionGranted(
                context,
                XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY
            )
        val allowedToStartActivityFromBackground =
            isPermissionToStartActivityFromBackgroundGranted || !isMiUI


        if (phoneAccountHandle != null && allowedToStartActivityFromBackground) {
            val extras = Bundle().apply {
                putString(CallConstants.EXTRA_ROOM_ALIAS, call.roomAlias)
                putString(CallConstants.EXTRA_NAME, call.displayName)
                putParcelable(
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri.fromParts(
                        PhoneAccount.SCHEME_TEL,
                        call.phone,   // must be digits or +E.164
                        null
                    )
                )
                putString(
                    CallConstants.EXTRA_CALL_TYPE,
                    call.callType.name
                )
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            }

            try {
                telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
            } catch (e: SecurityException) {
                Log.d(TAG, "Permission not granted. e = $e")
                // If there is no permission to start telecom call, we should start self-managed call
                context.sendSelfManagedIncomingCallBroadcast(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType
                )
                return
            } catch (e: Exception) {
                Log.d(TAG, "exception = $e")
                return
            }
        } else {
            context.sendSelfManagedIncomingCallBroadcast(
                displayName = call.displayName,
                phone = call.phone,
                roomAlias = call.roomAlias,
                callType = call.callType
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun tryToStartSystemOutgoingCall(call: Call): Boolean {
        if (!hasCallPhonePermission()) return false
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = tryToGetPhoneAccountHandle(telecomManager)

        if (phoneAccountHandle != null) {
            val uri = Uri.fromParts(
                PhoneAccount.SCHEME_TEL,
                call.phone, // must be digits or +E.164
                null
            )

            val extras = Bundle().apply {
                putParcelable(
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    phoneAccountHandle
                )

                putBundle(
                    TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS,
                    Bundle().apply {
                        putString(CallConstants.EXTRA_ROOM_ALIAS, call.roomAlias)
                        putString(CallConstants.EXTRA_NAME, call.displayName)
                        putString(CallConstants.EXTRA_CALL_TYPE, call.callType.name)
                    }
                )
            }

            try {
                // Start telecom call
                telecomManager.placeCall(uri, extras)
                return true
            } catch (e: SecurityException) {
                Log.d(TAG, "Permission not granted. e = $e")
                // If there no permission to start telecom call, we should start self-managed call
                context.sendSelfManagedOutgoingCallBroadcast(
                    displayName = call.displayName,
                    phone = call.phone,
                    roomAlias = call.roomAlias,
                    callType = call.callType
                )
                return false
            } catch (e: Exception) {
                Log.d(TAG, "exception = $e")
                return false
            }
        } else {
            return false
        }
    }


    @SuppressLint("MissingPermission")
    private fun tryToGetPhoneAccountHandle(telecomManager: TelecomManager): PhoneAccountHandle? {
        if (!hasReadPhoneStatePermission()) return null

        return telecomManager.callCapablePhoneAccounts.firstOrNull {
            it.componentName == ComponentName(context, CallConnectionService::class.java)
        }
    }

    fun hasReadPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPhonePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Context.sendSelfManagedIncomingCallBroadcast(
        displayName: String,
        phone: String,
        roomAlias: String,
        callType: CallType
    ) {
        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
        callIntent.putExtra(
            CallConstants.EXTRA_ACTION,
            CallAction.PlaceIncomingCall(
                displayName = displayName,
                phone = phone,
                roomAlias = roomAlias,
                callType = callType
            ),
        )
        sendBroadcast(callIntent)
    }

    private fun Context.sendSelfManagedOutgoingCallBroadcast(
        displayName: String,
        phone: String,
        roomAlias: String,
        callType: CallType
    ) {
        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
        callIntent.putExtra(
            CallConstants.EXTRA_ACTION,
            CallAction.PlaceOutgoingCall(
                displayName = displayName,
                phone = phone,
                roomAlias = roomAlias,
                callType = callType,
                isMuted = applicationContext.initialIsMuted(),
                isCameraOn = applicationContext.initialIsCameraOn(callType)
            ),
        )
        sendBroadcast(callIntent)
    }

    companion object {
        private const val TAG = "CallHandler"
    }
}