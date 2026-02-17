package space.livedigital.example.calls.internal.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.service.CallNotificationManager
import space.livedigital.example.calls.internal.repository.CallRepository

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
                CallConstants.EXTRA_ACTION,
                CallAction::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(CallConstants.EXTRA_ACTION)
        }
}