package space.livedigital.example.calls

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.telecom.DisconnectCause
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.broadcasts.CallBroadcast
import space.livedigital.example.calls.internal.service.CallService

class CallActivity : ComponentActivity() {

    private val viewModel by viewModel<CallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCallActivity()
        val action = extractAction()
        handleAnswerAction(action)
        handleOutgoingCallAction(action)
        setupContent()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            viewModel.onCallFinishedBySystem(DisconnectCause(DisconnectCause.LOCAL))
            stopService(Intent(this, CallService::class.java))
        }
    }

    private fun setupCallActivity() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguardManager = getSystemService<KeyguardManager>()
        keyguardManager?.requestDismissKeyguard(this, null)
    }

    private fun extractAction(): CallActivityAction? {
        val action = with(intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(
                    CallConstants.EXTRA_ACTION,
                    CallActivityAction::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(CallConstants.EXTRA_ACTION)
            }
        }
        return action
    }

    private fun handleAnswerAction(action: CallActivityAction?) {
        if (action is CallActivityAction.Answer) {
            val callIntent = Intent(applicationContext, CallBroadcast::class.java)
            callIntent.putExtra(
                CallConstants.EXTRA_ACTION,
                CallAction.Answer,
            )
            sendBroadcast(callIntent)
            val intent = Intent(this, CallService::class.java).apply {
                this.action = CallConstants.ACTION_UPDATE_CALL
            }
            startForegroundService(intent)
        }
    }

    private fun handleOutgoingCallAction(action: CallActivityAction?) {
        if (action is CallActivityAction.OutgoingCall) {
            val intent = Intent(this, CallService::class.java).apply {
                this.action = CallConstants.ACTION_OUTGOING_CALL
                putExtra(CallConstants.EXTRA_NAME, action.callerName)
                putExtra(CallConstants.EXTRA_NUMBER, action.phoneNumber)
                putExtra(CallConstants.EXTRA_ROOM_ALIAS, action.roomAlias)
            }
            startForegroundService(intent)
        }
    }

    private fun setupContent() {
        setContent {
            MaterialTheme {
                LaunchedEffect(viewModel.events) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ScreenEvent.CreateContact -> {
                                openContacts(event.callerName, event.phone)
                            }
                        }
                    }
                }

                val state = viewModel.state.collectAsStateWithLifecycle()

                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    CallScreen(
                        state = state,
                        onCallFinished = {
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    private fun openContacts(caller: String, number: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, caller)
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
            putExtra(
                ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            )
        }
        startActivity(intent)
    }


}

sealed interface CallActivityAction : Parcelable {

    @Parcelize
    object Answer : CallActivityAction

    @Parcelize
    data class OutgoingCall(
        val callerName: String,
        val phoneNumber: String,
        val roomAlias: String
    ) : CallActivityAction
}