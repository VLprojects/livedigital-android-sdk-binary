package space.livedigital.example.calls.internal.broadcasts

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.internal.service.CallService
import space.livedigital.example.calls.telecom.CallHandler

class CallBroadcast : BroadcastReceiver(), KoinComponent {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        // Get the action or skip if none
        val action = intent.getTelecomCallAction() ?: return
        val repository = CallRepository.instance ?: CallRepository.create()
        repository.dispatchCallAction(action)
        when (action) {
            is CallAction.PlaceIncomingCall -> {
                context.startCallService()
            }

            is CallAction.PlaceOutgoingCall -> {
                val isStarted = CallHandler(context).tryToStartSystemOutgoingCall(
                    Call.Actual(
                        displayName = action.displayName,
                        phone = action.phone,
                        roomAlias = action.roomAlias
                    )
                )

                // We don't need to start call service if system successfully start outgoing call
                if (!isStarted) {
                    context.startCallService()
                }
            }

            else -> Unit
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
        startForegroundService(intent)
    }
}