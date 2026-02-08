package space.livedigital.example.calls.utils

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.PermissionChecker
import space.livedigital.example.R
import space.livedigital.example.calls.CallAction
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.CallState

class CallNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    fun notifyAboutCall(
        displayName: String,
        address: String,
        isActive: Boolean
    ) {

    }

    /**
     * Updates, creates or dismisses a CallStyle notification based on the given [TelecomCall]
     */
    fun updateCallNotification(call: CallState) {
        // If notifications are not granted, skip it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }

        // Ensure that the channel is created
        createNotificationChannels()

        // Update or dismiss notification
        when (call) {
            CallState.None, is CallState.Unregistered -> {
                notificationManager.cancel(TELECOM_NOTIFICATION_ID)
            }

            is CallState.Registered -> {
                val notification = createNotification(
                    call.callAttributes.displayName.toString(),
                    call.callAttributes.address.toString(),
                    call.isActive
                )
                notificationManager.cancel(TELECOM_NOTIFICATION_ID)
                notificationManager.notify(TELECOM_NOTIFICATION_ID, notification)
            }
        }
    }

    fun createForegroundNotification(call: CallState.Registered): Notification {
        return createNotification(
            call.callAttributes.displayName.toString(),
            call.callAttributes.address.toString(),
            call.isActive
        )
    }

    fun createForegroundNotification(
        displayName: String,
        address: String,
        isActive: Boolean
    ): Notification {
        return createNotification(displayName, address, isActive)
    }

    fun createIdleNotification(): Notification {
        return NotificationCompat.Builder(context, TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setContentTitle("Call in progress")
            .setOngoing(true)
            .build()
    }

    private fun createNotification(
        displayName: String,
        address: String,
        isActive: Boolean
    ): Notification {
        // To display the caller information
        val caller = Person.Builder()
            .setName(displayName)
            .setUri(address)
            .setImportant(true)
            .build()

        // Defines the full screen notification activity or the activity to launch once the user taps
        // on the notification
        val contentIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ Intent(context, CallActivity::class.java),
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Define the call style based on the call state and set the right actions
        val callIntent = Intent(context, CallActivity::class.java)
        callIntent.putExtra(
            TELECOM_NOTIFICATION_ACTION,
            CallAction.Answer,
        )

        val answerIntent = PendingIntent.getActivity(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isIncoming = !isActive
        val callStyle = if (isIncoming) {
            NotificationCompat.CallStyle.forIncomingCall(
                caller,
                getPendingIntent(
                    CallAction.Disconnect(
                        DisconnectCause(DisconnectCause.REJECTED),
                    ),
                ),
                answerIntent,
            )
        } else {
            NotificationCompat.CallStyle.forOngoingCall(
                caller,
                getPendingIntent(
                    CallAction.Disconnect(
                        DisconnectCause(DisconnectCause.LOCAL),
                    ),
                ),
            )
        }
        val channelId = if (isIncoming) {
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID
        } else {
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setOngoing(true)
            .setStyle(callStyle)

        return builder.build()
    }

    /**
     * Creates a PendingIntent for the given [TelecomCallAction]. Since the actions are parcelable
     * we can directly pass them as extra parameters in the bundle.
     */
    private fun getPendingIntent(action: CallAction): PendingIntent {
        val callIntent = Intent(context, CallBroadcast::class.java)
        callIntent.putExtra(
            TELECOM_NOTIFICATION_ACTION,
            action,
        )

        return PendingIntent.getBroadcast(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannels() {
        val incomingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName("Incoming calls")
            .setDescription("Handles the notifications when receiving a call")
            .setSound(
                null,
                null
            )
            .setVibrationEnabled(false)
            .build()

        val ongoingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        ).setName("Ongoing calls").setDescription("Displays the ongoing call notifications").build()

        notificationManager.createNotificationChannelsCompat(
            listOf(
                incomingChannel,
                ongoingChannel
            )
        )
    }

    internal companion object {
        const val TELECOM_NOTIFICATION_ID = 200
        const val TELECOM_NOTIFICATION_ACTION = "telecom_action"
        const val TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID = "telecom_incoming_channel"
        const val TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID = "telecom_ongoing_channel"
    }
}