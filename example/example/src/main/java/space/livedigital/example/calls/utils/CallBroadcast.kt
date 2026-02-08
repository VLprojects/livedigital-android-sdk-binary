package space.livedigital.example.calls.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.CallAction
import space.livedigital.example.calls.CallState

class CallBroadcast : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        // Get the action or skip if none
        val action = intent.getTelecomCallAction() ?: return
        val repository = CallRepository.instance ?: CallRepository.create(context)
        val call = repository.currentCallState.value

        if (call is CallState.Registered) {
            // If the call is still registered perform action
            call.processAction(action)
        } else {
            // Otherwise probably something went wrong and the notification is wrong.
            CallNotificationManager(context).updateCallNotification(call)
        }
    }

    /**
     * Get the [TelecomCallAction] parcelable object from the intent bundle.
     */
    private fun Intent.getTelecomCallAction(): CallAction? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(
                CallNotificationManager.TELECOM_NOTIFICATION_ACTION,
                CallAction::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(CallNotificationManager.TELECOM_NOTIFICATION_ACTION)
        }
}