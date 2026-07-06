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
import space.livedigital.example.AuthStorage
import space.livedigital.example.BuildConfig
import space.livedigital.example.calls.broadcasts.CallBroadcast
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.services.CallConnectionService

class CallHandler(private val context: Context) {

    /**
     * Entry point for dialing a number from this app's own UI: mints the outbound [Call] and
     * routes it through the regular telecom path ([CallBroadcast] → system dialer or
     * self-managed CallService). The channel join and `engine.initiateCall()` happen in
     * CallViewModel once the call screen opens.
     */
    fun placeOutgoingCall(calleePhoneNumber: String) {
        val call = buildOutgoingCall(calleePhoneNumber) ?: return
        context.sendSelfManagedOutgoingCallBroadcast(call)
    }

    /**
     * Builds a locally-signed outbound [Call] for a number that wasn't dialed through this app's
     * own UI — e.g. redialed from the system call log, or dialed via the native Phone app while
     * this app's account is the default outgoing account. [onCreateOutgoingConnection][
     * space.livedigital.example.calls.services.CallConnectionService.onCreateOutgoingConnection]
     * only receives the destination address in that case, so the signaling token (which is what
     * actually tells the backend to originate the PSTN leg) has to be minted here instead of
     * being carried over from [CallConstants.EXTRA_SIGNALING_TOKEN].
     */
    fun buildOutgoingCall(calleePhoneNumber: String): Call.Actual? {
        val authStorage = AuthStorage.instance ?: AuthStorage.create(context)
        val callerPhoneNumber = authStorage.phoneNumber

        if (callerPhoneNumber.isBlank()) {
            return null
        }

        val signalingToken = OutboundCallTokenGenerator(
            devicesApiKey = BuildConfig.DEVICES_API_KEY,
            signalingTokenKey = BuildConfig.SIGNALING_TOKEN_KEY,
            deviceId = authStorage.deviceId
        ).makeOutboundCallToken(
            callerPhoneNumber = callerPhoneNumber,
            calleePhoneNumber = calleePhoneNumber
        )

        return Call.Actual(
            displayName = calleePhoneNumber,
            phone = calleePhoneNumber,
            signalingToken = signalingToken,
            callType = CallType.AUDIO
        )
    }

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
                putString(CallConstants.EXTRA_SIGNALING_TOKEN, call.signalingToken)
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
                context.sendSelfManagedIncomingCallBroadcast(call)
                return
            } catch (e: Exception) {
                Log.d(TAG, "exception = $e")
                return
            }
        } else {
            context.sendSelfManagedIncomingCallBroadcast(call)
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
                        putString(CallConstants.EXTRA_SIGNALING_TOKEN, call.signalingToken)
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
                context.sendSelfManagedOutgoingCallBroadcast(call)
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

    private fun Context.sendSelfManagedIncomingCallBroadcast(call: Call) {
        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
        callIntent.putExtra(
            CallConstants.EXTRA_ACTION,
            CallAction.PlaceIncomingCall(call = call),
        )
        sendBroadcast(callIntent)
    }

    private fun Context.sendSelfManagedOutgoingCallBroadcast(call: Call) {
        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
        callIntent.putExtra(
            CallConstants.EXTRA_ACTION,
            CallAction.PlaceOutgoingCall(
                call = call,
                isMuted = applicationContext.initialIsMuted(),
                isCameraOn = applicationContext.initialIsCameraOn(call.callType)
            ),
        )
        sendBroadcast(callIntent)
    }

    companion object {
        private const val TAG = "CallHandler"
    }
}