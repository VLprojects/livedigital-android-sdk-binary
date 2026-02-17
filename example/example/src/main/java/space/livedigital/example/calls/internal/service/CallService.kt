package space.livedigital.example.calls.internal.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.repository.CallRepository

class CallService : Service() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
    private var notificationManager: CallNotificationManager? = null
    private var repository: CallRepository? = null

    private var ringtone: Ringtone? = null
    private var vibrator: Any? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = CallNotificationManager(applicationContext)
        repository = CallRepository.instance ?: CallRepository.create(applicationContext)

        // Observe call status updates once the call is registered and update the service
        repository?.currentCallState
            ?.onEach { call ->
                updateServiceState(call)
            }
            ?.onCompletion {
                // If the scope is completed stop the service
                stopSelf()
            }
            ?.launchIn(scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove notification and clean resources
        scope.cancel()
        notificationManager?.updateCallNotification(CallState.None)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val call = repository?.currentCallState?.value
        val notificationManager = notificationManager ?: CallNotificationManager(applicationContext)
            .also {
                notificationManager = it
            }

        val notification = if (call != null && call is CallState.Registered) {
            notificationManager.createForegroundNotification(call)
        } else {
            notificationManager.createIdleNotification()
        }

        val serviceType = getServiceType(intent)

        try {
            ServiceCompat.startForeground(
                this,
                CallNotificationManager.TELECOM_NOTIFICATION_ID,
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

        if (intent != null) {
            when (intent.action) {
                CallConstants.ACTION_INCOMING_CALL -> registerCall(intent)
            }
        }

        return START_STICKY
    }

    private fun getServiceType(intent: Intent?): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return 0
        }

        if (intent == null || intent.action == null) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }

        if (intent.action == CallConstants.ACTION_INCOMING_CALL) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }

        if (isRecordAudioPermissionGranted()) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        return FOREGROUND_SERVICE_TYPE_PHONE_CALL
    }

    private fun registerCall(intent: Intent) {
        // If we have an ongoing call ignore command
        if (repository?.currentCallState?.value is CallState.Registered) {
            return
        }

        runCatching {
            val name = intent.getStringExtra(CallConstants.EXTRA_NAME) ?: return
            val phoneNumber = intent.getStringExtra(CallConstants.EXTRA_NUMBER) ?: return
            val phoneNumberUri = "tel:$phoneNumber".toUri()
            val roomAlias = intent.getStringExtra(CallConstants.EXTRA_ROOM_ALIAS) ?: return

            scope.launch {
                repository?.registerCall(name, roomAlias, phoneNumberUri)
            }
        }.onFailure {
            return
        }
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        val permissionResult =
            applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    private fun startRingtoneAndVibration() {
        turnOnBluetoothAndCommunicationModeIfPossible()
        startRingtone()
        startVibration()
    }

    private fun turnOnBluetoothAndCommunicationModeIfPossible() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (audioManager.isBluetoothScoAvailableOffCall) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices.filter {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

                if (devices.isEmpty()) return

                audioManager.setCommunicationDevice(devices.first())
            } else {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
        }
    }

    private fun startRingtone() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.audioAttributes = audioAttributes
            ringtone?.isLooping = true
            ringtone?.play()
        }
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
            vib.vibrate(effect, vibrationAttributes)
        }
    }

    private fun stopRingtoneAndVibration() {
        resetAudioMode()
        stopRingtone()
        stopVibration()
    }

    private fun resetAudioMode() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        audioManager.mode = AudioManager.MODE_NORMAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
        }
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

    private fun updateServiceState(call: CallState?) {
        if (call == null) return

        notificationManager?.updateCallNotification(call)

        when (call) {
            is CallState.Unregistered -> {
                stopRingtoneAndVibration()
                stopSelf()
            }

            CallState.None -> {}
            is CallState.Registered -> {
                if (call.isIncoming() && !call.isActive) {
                    startRingtoneAndVibration()
                } else {
                    stopRingtoneAndVibration()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}