package space.livedigital.example.calls.services

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.broadcasts.CallBroadcast
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction.Activate
import space.livedigital.example.calls.entities.CallAction.Answer
import space.livedigital.example.calls.entities.CallAction.Disconnect
import space.livedigital.example.calls.entities.CallAction.PlaceActiveCall
import space.livedigital.example.calls.entities.CallCommand
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.utils.CallConverter
import space.livedigital.example.calls.utils.initialIsMuted

class CallService : LifecycleService() {

    /**
     *  Can the call be successfully answered??
     *  TIP: We would check the connection/call state to see if we can answer a call
     *  Example you may need to wait for another call to hold.
     **/
    val onIsCallAnswered: suspend (type: Int) -> Unit = {
        repository?.currentCallState?.value?.let { callState ->
            repository?.dispatchCallAction(
                Answer(
                    call = callState.call,
                    isMuted = applicationContext.initialIsMuted()
                )
            )
        }
    }

    /**
     * Can the call perform a disconnect
     */
    val onIsCallDisconnected: suspend (cause: DisconnectCause) -> Unit = { disconnectCause ->
        repository?.currentCallState?.value?.let { callState ->
            repository?.dispatchCallAction(
                Disconnect(call = callState.call, cause = disconnectCause)
            )
        }
    }

    /**
     *  Check is see if we can make the call active.
     *  Other calls and state might stop us from activating the call
     */
    val onIsCallActive: suspend () -> Unit = {
        repository?.currentCallState?.value?.let { callState ->
            repository?.dispatchCallAction(Activate(call = callState.call))
        }
    }

    /**
     * Check to see if we can make the call inactivate
     */
    val onIsCallInactive: suspend () -> Unit = {
        // Make call inactive
    }

    private var notificationManager: CallNotificationManager? = null
    private var repository: CallRepository? = null
    private var callsManager: CallsManager? = null
    private var callControlScope: CallControlScope? = null

    private var ringtone: Ringtone? = null
    private var vibrator: Any? = null

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = CallNotificationManager(applicationContext)
        repository = CallRepository.instance ?: CallRepository.create()
        // Manager to add call in system (without integration with dialer app)
        callsManager = CallsManager(applicationContext).apply {
            registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING
            )
        }

        repository?.currentCallState
            ?.onEach { call ->
                updateServiceState(call)
            }
            ?.onCompletion {
                stopSelf()
            }
            ?.launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        repository = null
        notificationManager = null
        callsManager = null
        callControlScope = null
    }

    private fun updateServiceState(callState: CallState) {
        lifecycleScope.launch {
            when (callState) {
                is CallState.Answered -> {
                    stopRingtoneAndVibration()
                    val result = callControlScope?.answer(
                        callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL
                    )

                    if (result is CallControlResult.Success) {
                        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
                        callIntent.putExtra(
                            CallConstants.EXTRA_ACTION,
                            PlaceActiveCall(call = callState.call),
                        )
                        sendBroadcast(callIntent)
                    }
                }

                is CallState.Activated -> {
                    stopRingtoneAndVibration()
                    val result = callControlScope?.setActive()

                    if (result is CallControlResult.Success) {
                        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
                        callIntent.putExtra(
                            CallConstants.EXTRA_ACTION,
                            PlaceActiveCall(call = callState.call),
                        )
                        sendBroadcast(callIntent)
                    }
                }

                is CallState.Active -> {
                    val notification = notificationManager?.createOngoingCallNotification(callState)
                        ?: return@launch
                    val serviceType = getServiceType(callState)

                    try {
                        ServiceCompat.startForeground(
                            this@CallService,
                            CallNotificationManager.NOTIFICATION_ID,
                            notification,
                            serviceType
                        )
                    } catch (exception: IllegalStateException) {
                        // Solution from https://issuetracker.google.com/issues/307329994#comment86
                        @Suppress("InstanceOfCheckForException")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            exception is ForegroundServiceStartNotAllowedException
                        ) {
                            stopSelf()
                        }
                    }
                    stopRingtoneAndVibration()
                }


                is CallState.Ended -> {
                    stopRingtoneAndVibration()
                    callControlScope?.disconnect(
                        disconnectCause = convertToSafeDisconnectCause(
                            callState.disconnectCause.code
                        )
                    )
                    callControlScope = null
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                CallState.Idle -> {
                    stopRingtoneAndVibration()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                is CallState.Incoming -> {
                    val notification =
                        notificationManager?.createIncomingCallNotification(callState)
                            ?: return@launch
                    val serviceType = getServiceType(callState)

                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        ServiceCompat.startForeground(
                            this@CallService,
                            CallNotificationManager.NOTIFICATION_ID,
                            notification,
                            serviceType
                        )
                    } catch (exception: IllegalStateException) {
                        // Solution from https://issuetracker.google.com/issues/307329994#comment86
                        @Suppress("InstanceOfCheckForException")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            exception is ForegroundServiceStartNotAllowedException
                        ) {
                            stopSelf()
                        }
                    }
                    startRingtoneAndVibration()
                    if (callControlScope == null) {
                        registerCall(
                            displayName = callState.call.displayName,
                            phone = callState.call.phone,
                            isIncoming = true
                        )
                    }
                }

                is CallState.Missed -> {
                    stopRingtoneAndVibration()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationManager?.showMissedCallNotification(callState)
                    callControlScope?.disconnect(
                        disconnectCause = convertToSafeDisconnectCause(
                            callState.disconnectCause.code
                        )
                    )
                    callControlScope = null
                    stopSelf()
                }

                is CallState.Outgoing -> {
                    val notification = notificationManager?.createOngoingCallNotification(callState)
                        ?: return@launch
                    val serviceType = getServiceType(callState)

                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        ServiceCompat.startForeground(
                            this@CallService,
                            CallNotificationManager.NOTIFICATION_ID,
                            notification,
                            serviceType
                        )
                    } catch (exception: IllegalStateException) {
                        // Solution from https://issuetracker.google.com/issues/307329994#comment86
                        @Suppress("InstanceOfCheckForException")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            exception is ForegroundServiceStartNotAllowedException
                        ) {
                            stopSelf()
                        }
                    }
                    stopRingtoneAndVibration()
                    if (callControlScope == null) {
                        // The room join and engine.initiateCall() live in CallViewModel, which
                        // only exists while CallActivity is shown — the ongoing-call
                        // notification alone would leave the call ringing forever. Must happen
                        // before registerCall: addCall suspends until the call session ends.
                        startCallActivity()
                        registerCall(
                            displayName = callState.call.displayName,
                            phone = callState.call.phone,
                            isIncoming = true
                        )
                    }
                }
            }
        }
    }

    private fun startCallActivity() {
        val intent = Intent(applicationContext, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    fun convertToSafeDisconnectCause(internalReasonCode: Int): DisconnectCause {
        return when (internalReasonCode) {
            DisconnectCause.LOCAL -> DisconnectCause(DisconnectCause.LOCAL)
            DisconnectCause.REMOTE -> DisconnectCause(DisconnectCause.REMOTE)
            DisconnectCause.MISSED -> DisconnectCause(DisconnectCause.MISSED)
            DisconnectCause.REJECTED -> DisconnectCause(DisconnectCause.REJECTED)

            // Map all other cases (like 11 / CALL_PULLED) to an allowed fallback
            else -> {
                // For example, if a call was pulled to another device,
                // from this device's perspective, it's a LOCAL termination.
                DisconnectCause(DisconnectCause.LOCAL, "Call transferred or pulled")
            }
        }
    }

    private fun getServiceType(callState: CallState): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return 0
        }

        if (callState is CallState.Incoming) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }

        if (isRecordAudioPermissionGranted()) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        return FOREGROUND_SERVICE_TYPE_PHONE_CALL
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        val permissionResult =
            applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun registerCall(
        displayName: String,
        phone: String,
        isIncoming: Boolean
    ) {
        val callAttributes = CallAttributesCompat(
            displayName = displayName,
            address = Uri.fromParts(
                PhoneAccount.SCHEME_TEL,
                phone,   // must be digits or +E.164
                null
            ),
            callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            }
        )

        try {
            callsManager?.addCall(
                callAttributes,
                onIsCallAnswered,
                onIsCallDisconnected,
                onIsCallActive,
                onIsCallInactive
            ) {
                callControlScope = this

                launch {
                    availableEndpoints.collect { callEndpoints ->
                        val generalCallEndpoints = callEndpoints.map { callEndpoint ->
                            GeneralCallEndpoint(
                                id = callEndpoint.identifier.toString(),
                                name = callEndpoint.name.toString(),
                                type = CallConverter.convertToGeneralEndpointType(
                                    callEndpoint.type
                                ),
                                rawEndpoint = callEndpoint
                            )
                        }
                        repository?.onCallEndpointsChanged(generalCallEndpoints)
                    }
                }

                launch {
                    currentCallEndpoint.collect { callEndpoint ->
                        val generalCallEndpoints = GeneralCallEndpoint(
                            id = callEndpoint.identifier.toString(),
                            name = callEndpoint.name.toString(),
                            type = CallConverter.convertToGeneralEndpointType(callEndpoint.type),
                            rawEndpoint = callEndpoint
                        )
                        repository?.onCallCurrentEndpointChanged(generalCallEndpoints)
                    }
                }
                launch {
                    repository?.commands?.collect { callCommand ->
                        when (callCommand) {
                            is CallCommand.ChangeEndpoint -> {
                                callControlScope?.requestEndpointChange(
                                    callCommand.endpoint.rawEndpoint as CallEndpointCompat
                                )
                            }
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e("CallService", "add call finished with error $exception")
        }
    }

    private fun startRingtoneAndVibration() {
        setAudioManagerModeToRingtone()
        startRingtone()
        startVibration()
    }

    private fun setAudioManagerModeToRingtone() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_RINGTONE
    }

    private fun startRingtone() {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        ringtone?.audioAttributes = audioAttributes
        ringtone?.isLooping = true
        ringtone?.play()
    }

    private fun startVibration() {
        val vibrationAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibPattern = longArrayOf(0, 1000, 1000)
        val effect = VibrationEffect.createWaveform(vibPattern, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val vibManager = getSystemService(VibratorManager::class.java)
            vibrator = vibManager
            val vibAttributes = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_RINGTONE)
                .build()

            vibManager?.defaultVibrator?.vibrate(effect, vibAttributes)
        } else {
            val vib = getSystemService(Vibrator::class.java)
                ?: getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator = vib
            vib.vibrate(effect, vibrationAttributes)
        }
    }

    private fun stopRingtoneAndVibration() {
        setAudioManagerModeToNormal()
        stopRingtone()
        stopVibration()
    }

    private fun setAudioManagerModeToNormal() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun stopRingtone() {
        ringtone?.stop()
        ringtone = null
    }

    private fun stopVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (vibrator as? VibratorManager)?.defaultVibrator?.cancel()
        } else {
            (vibrator as? Vibrator)?.cancel()
        }
        vibrator = null
    }
}