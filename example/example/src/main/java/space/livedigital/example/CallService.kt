package space.livedigital.example

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()

        return START_STICKY
    }

    private fun startForeground() {
        val notification =
            createNotification(
                context = applicationContext,
                channelId = App.CALL_CHANNEL_ID,
                titleText = "Call started"
            )

        /**
         * Обработка краша при перезапуске сервиса системой из background из-за START_STICKY.
         * Подробнее в официальном issue Google:
         * https://issuetracker.google.com/issues/307329994#comment128
         */
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                getServiceType()
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
    }

    fun createNotification(
        context: Context,
        channelId: String,
        titleText: String,
        notificationRequestCode: Int? = null,
        notificationIntent: Intent? = null
    ): Notification {
        val builder = Notification.Builder(context, channelId)
            .setContentTitle(titleText)

        if (notificationRequestCode != null && notificationIntent != null) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationRequestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)

        return builder.build()
    }

    private fun getServiceType(): Int {
        if (isRecordAudioPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        return FOREGROUND_SERVICE_TYPE_PHONE_CALL
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        val permissionResult =
            applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val NOTIFICATION_ID = 2231
    }
}