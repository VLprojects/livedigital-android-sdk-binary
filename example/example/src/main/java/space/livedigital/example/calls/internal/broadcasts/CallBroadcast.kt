package space.livedigital.example.calls.internal.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.internal.service.CallService

class CallBroadcast : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("xd", "onReceive $intent")
        // Get the action or skip if none
        val action = intent.getTelecomCallAction() ?: return
        val repository = CallRepository.instance ?: CallRepository.create()
        repository.dispatchCallAction(action)
        if (action is CallAction.PlaceIncomingCall || action is CallAction.PlaceOutgoingCall) {
            context.startCallService()
        }
    }

    /**
     * Get the [CallAction] parcelable object from the intent bundle.
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

    private fun Context.startCallService() {
        val intent = Intent(this, CallService::class.java)
        startService(intent)
    }
}