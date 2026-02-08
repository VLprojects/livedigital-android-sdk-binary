package space.livedigital.example.calls.utils

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.livedigital.example.calls.CallState
import kotlin.jvm.java

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
        Log.d("xd", "onStartCommand $intent")
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
            // Решение взято из https://issuetracker.google.com/issues/307329994#comment86
            @Suppress("InstanceOfCheckForException")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                exception is ForegroundServiceStartNotAllowedException
            ) {
                stopSelf()
            }
        }

        if (intent != null) {
            when (intent.action) {
                ACTION_INCOMING_CALL -> registerCall(intent)
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

        if (intent.action == ACTION_INCOMING_CALL) {
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
            val name = intent.getStringExtra(EXTRA_NAME) ?: return
            val phoneNumber = intent.getStringExtra(EXTRA_NUMBER) ?: return
            val phoneNumberUri = "tel:$phoneNumber".toUri()
            val roomAlias = intent.getStringExtra(EXTRA_ROOM_ALIAS) ?: return

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
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        ringtone?.isLooping = true
        ringtone?.play()

        val vibPattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibManager = getSystemService(VibratorManager::class.java)
            vibrator = vibManager
            val effect = VibrationEffect.createWaveform(vibPattern, 0)
            vibManager?.defaultVibrator?.vibrate(effect)
        } else {
            val vib = getSystemService(Vibrator::class.java)
                ?: getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator = vib
            val effect = VibrationEffect.createWaveform(vibPattern, 0)
            vib.vibrate(effect)
        }
    }

    private fun stopRingtoneAndVibration() {
        ringtone?.stop()
        ringtone = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    companion object {
        internal const val EXTRA_NAME: String = "extra_name"
        internal const val EXTRA_NUMBER: String = "extra_number"
        internal const val EXTRA_ROOM_ALIAS: String = "extra_room_alias"
        internal const val ACTION_INCOMING_CALL = "incoming_call"
        internal const val ACTION_UPDATE_CALL = "update_call"
    }
}