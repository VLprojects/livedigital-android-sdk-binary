package space.livedigital.example.calls.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.PermissionChecker
import space.livedigital.example.R
import space.livedigital.example.calls.CallActivity
import space.livedigital.example.calls.broadcasts.CallBroadcast
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallActivityAction
import space.livedigital.example.calls.entities.CallState

internal class CallNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    fun createIncomingCallNotification(callState: CallState.Incoming): Notification? {
        if (!hasNotificationPermission()) return null
        createNotificationChannels()

        val contentIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP
            putExtra(
                CallConstants.EXTRA_ACTION,
                CallActivityAction.Answer(call = callState.call)
            )
        }

        val pendingAnswerIntent = PendingIntent.getActivity(
            context,
            callState.call.phone.hashCode() + 1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val caller = Person.Builder()
            .setName(callState.call.displayName)
            .setUri(
                Uri.fromParts(
                    PhoneAccount.SCHEME_TEL,
                    callState.call.phone,   // must be digits or +E.164
                    null
                ).toString()
            )
            .setImportant(true)
            .build()

        val style = NotificationCompat.CallStyle.forIncomingCall(
            caller,
            getActionPendingIntent(
                CallAction.Disconnect(
                    displayName = callState.call.displayName,
                    phone = callState.call.phone,
                    roomAlias = callState.call.roomAlias,
                    cause = DisconnectCause(DisconnectCause.LOCAL)
                )
            ),
            pendingAnswerIntent
        )

        val notification = NotificationCompat.Builder(context, INCOMING_CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_push_notification)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingContentIntent)
            .setFullScreenIntent(pendingContentIntent, true)
            .setOngoing(true)
            .setStyle(style)
            .build()

        return notification
    }

    @SuppressLint("MissingPermission")
    fun createOngoingCallNotification(callState: CallState): Notification? {
        if (!hasNotificationPermission()) return null
        createNotificationChannels()

        val caller = Person.Builder()
            .setName(callState.call.displayName)
            .setUri(
                Uri.fromParts(
                    PhoneAccount.SCHEME_TEL,
                    callState.call.phone,   // must be digits or +E.164
                    null
                ).toString()
            )
            .setImportant(true)
            .build()

        val style = NotificationCompat.CallStyle.forOngoingCall(
            caller,
            getActionPendingIntent(
                CallAction.Disconnect(
                    displayName = callState.call.displayName,
                    phone = callState.call.phone,
                    roomAlias = callState.call.roomAlias,
                    cause = DisconnectCause(DisconnectCause.LOCAL)
                )
            )
        )

        val contentIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ONGOING_CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_push_notification)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingContentIntent)
            .setOngoing(true)
            .setStyle(style)
            .build()

        return notification
    }

    @SuppressLint("MissingPermission")
    fun createCallAudioServiceNotification(): Notification? {
        if (!hasNotificationPermission()) return null
        createNotificationChannels()

        val contentIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, ONGOING_CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_push_notification)
            .setContentTitle(context.getString(R.string.label_microphone_is_active))
            .setContentText(
                context.getString(R.string.description_call_is_using_audio_in_background)
            )
            .setContentIntent(pendingContentIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun showMissedCallNotification(callState: CallState.Missed) {
        if (!hasNotificationPermission()) return
        createNotificationChannels()

        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(
                CallConstants.EXTRA_ACTION,
                CallActivityAction.PlaceOutgoingCall(
                    callerName = callState.call.displayName,
                    phoneNumber = callState.call.phone,
                    roomAlias = callState.call.roomAlias
                )
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MISSED_CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_push_notification)
            .setContentTitle(callState.call.displayName)
            .setContentText("Missed call")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED)
    }

    private fun getActionPendingIntent(action: CallAction): PendingIntent {
        val intent = Intent(context, CallBroadcast::class.java).apply {
            putExtra(CallConstants.EXTRA_ACTION, action)
        }
        return PendingIntent.getBroadcast(
            context,
            intent.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildIncomingCallsChannel(),
                buildOngoingCallsChannel(),
                buildMissedCallsChannel()
            )
        )
    }

    private fun buildIncomingCallsChannel() = NotificationChannelCompat.Builder(
        INCOMING_CALLS_CHANNEL_ID,
        NotificationManagerCompat.IMPORTANCE_MAX
    )
        .setName("Incoming calls")
        .setDescription("Notifications for incoming calls")
        .setSound(null, null)
        .setVibrationEnabled(false)
        .build()

    private fun buildOngoingCallsChannel() = NotificationChannelCompat.Builder(
        ONGOING_CALLS_CHANNEL_ID,
        NotificationManagerCompat.IMPORTANCE_DEFAULT
    )
        .setName("Ongoing calls")
        .setSound(null, null)
        .setVibrationEnabled(false)
        .setDescription("Notifications for active calls")
        .build()

    private fun buildMissedCallsChannel() = NotificationChannelCompat.Builder(
        MISSED_CALLS_CHANNEL_ID,
        NotificationManagerCompat.IMPORTANCE_DEFAULT
    )
        .setName("Missed calls")
        .setDescription("Notifications for missed calls")
        .build()


    companion object {
        const val NOTIFICATION_ID = 200
        const val INCOMING_CALLS_CHANNEL_ID = "incoming_calls_channel"
        const val ONGOING_CALLS_CHANNEL_ID = "ongoing_calls_channel"
        const val MISSED_CALLS_CHANNEL_ID = "missed_calls_channel"
    }
}