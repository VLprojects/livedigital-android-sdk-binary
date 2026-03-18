package space.livedigital.example.calls.telecom.services

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import space.livedigital.example.calls.internal.service.CallNotificationManager

class CallConnectionAudioService : Service() {

    private var notificationManager: CallNotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = CallNotificationManager(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()

        return START_STICKY
    }

    private fun startForeground() {
        val notification = notificationManager?.createCallAudioServiceNotification() ?: return

        try {
            ServiceCompat.startForeground(
                this,
                CallNotificationManager.NOTIFICATION_ID,
                notification,
                getServiceType()
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
    }

    private fun getServiceType(): Int {
        if (isRecordAudioPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        return 0
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        val permissionResult =
            applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        return permissionResult == PackageManager.PERMISSION_GRANTED
    }
}