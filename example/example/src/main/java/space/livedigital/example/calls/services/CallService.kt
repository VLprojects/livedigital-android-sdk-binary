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
import androidx.core.telecom.CallsManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.livedigital.example.calls.broadcasts.CallBroadcast
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction.Activate
import space.livedigital.example.calls.entities.CallAction.Answer
import space.livedigital.example.calls.entities.CallAction.Disconnect
import space.livedigital.example.calls.entities.CallAction.PlaceActiveCall
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.repositories.CallRepository

internal class CallService : LifecycleService() {

    /**
     *  Can the call be successfully answered??
     *  TIP: We would check the connection/call state to see if we can answer a call
     *  Example you may need to wait for another call to hold.
     **/
    val onIsCallAnswered: suspend (type: Int) -> Unit = {
        repository?.currentCallState?.value?.let { callState ->
            repository?.dispatchCallAction(
                Answer(
                    displayName = callState.call.displayName,
                    phone = callState.call.phone,
                    roomAlias = callState.call.roomAlias
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
                Disconnect(
                    displayName = callState.call.displayName,
                    phone = callState.call.phone,
                    roomAlias = callState.call.roomAlias,
                    cause = disconnectCause
                )
            )
        }
    }

    /**
     *  Check is see if we can make the call active.
     *  Other calls and state might stop us from activating the call
     */
    val onIsCallActive: suspend () -> Unit = {
        repository?.currentCallState?.value?.let { callState ->
            repository?.dispatchCallAction(
                Activate(
                    displayName = callState.call.displayName,
                    phone = callState.call.phone,
                    roomAlias = callState.call.roomAlias
                )
            )
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
                capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                        CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
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
                            PlaceActiveCall(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias
                            ),
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
                            PlaceActiveCall(
                                displayName = callState.call.displayName,
                                phone = callState.call.phone,
                                roomAlias = callState.call.roomAlias
                            ),
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
                    callControlScope?.setActive()
                }


                is CallState.Ended -> {
                    stopRingtoneAndVibration()
                    callControlScope?.disconnect(disconnectCause = callState.disconnectCause)
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
                    callControlScope?.disconnect(disconnectCause = callState.disconnectCause)
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