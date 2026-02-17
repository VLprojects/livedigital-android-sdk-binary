package space.livedigital.example.calls.internal.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.PermissionChecker
import space.livedigital.example.R
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.broadcasts.CallBroadcast

class CallNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    fun notifyAboutMissedCall(callerName: String) {
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
        val notificationId = System.currentTimeMillis().toInt()
        val notification = createMissedCallNotification(callerName)
        notificationManager.notify(notificationId, notification)
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
                notificationManager.cancel(NOTIFICATION_ID)
            }

            is CallState.Registered -> {
                val notification = createCallNotification(
                    call.callAttributes.displayName.toString(),
                    call.callAttributes.address.toString(),
                    call.isActive
                )
                notificationManager.cancel(NOTIFICATION_ID)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    fun createForegroundNotification(call: CallState.Registered): Notification {
        return createCallNotification(
            call.callAttributes.displayName.toString(),
            call.callAttributes.address.toString(),
            call.isActive
        )
    }

    fun createIdleNotification(): Notification {
        return NotificationCompat.Builder(context, ONGOING_CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setContentTitle("Call in progress")
            .setOngoing(true)
            .build()
    }

    private fun createMissedCallNotification(callerName: String): Notification {
        return NotificationCompat.Builder(context, MISSED_CALLS_CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setContentTitle(callerName)
            .setContentText("Missed call")
            .build()
    }

    private fun createCallNotification(
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
            CallConstants.EXTRA_ACTION,
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
            INCOMING_CALLS_CHANNEL_ID
        } else {
            ONGOING_CALLS_CHANNEL_ID
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
            CallConstants.EXTRA_ACTION,
            action
        )

        return PendingIntent.getBroadcast(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannels() {
        val incomingChannel = buildIncomingCallsChannel()
        val ongoingChannel = buildOngoingCallsChannel()
        val missedChannel = buildMissedCallsChannel()

        notificationManager.createNotificationChannelsCompat(
            listOf(
                incomingChannel,
                ongoingChannel,
                missedChannel
            )
        )
    }

    private fun buildIncomingCallsChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            INCOMING_CALLS_CHANNEL_ID,
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

    private fun buildOngoingCallsChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            ONGOING_CALLS_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        )
            .setName("Ongoing calls")
            .setDescription("Displays the ongoing call notifications")
            .build()

    private fun buildMissedCallsChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            MISSED_CALLS_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Missed calls")
            .setDescription("Notifications for calls that were missed")
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()


    internal companion object {
        const val NOTIFICATION_ID = 200
        const val INCOMING_CALLS_CHANNEL_ID = "incoming_calls_channel"
        const val ONGOING_CALLS_CHANNEL_ID = "ongoing_calls_channel"
        const val MISSED_CALLS_CHANNEL_ID = "missed_calls_channel"
        const val MISSED_CALLS_GROUP_ID_PREFIX = "missed_calls_group_id_"
    }
}