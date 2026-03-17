package space.livedigital.example.calls

import android.app.ComponentCaller
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.DisconnectCause
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.broadcasts.CallBroadcast
import space.livedigital.example.ui.screens.CallScreen
import space.livedigital.example.ui.theme.AppTheme

class CallActivity : ComponentActivity() {

    private val viewModel by viewModel<CallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = createNavigationBarStyle())
        setupCallActivity()
        val action = extractAction()
        handleAnswerAction(action)
        handleOutgoingCallAction(action)
        setContent()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        setIntent(intent)
        val action = extractAction()
        handleAnswerAction(action)
        handleOutgoingCallAction(action)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = extractAction()
        handleAnswerAction(action)
        handleOutgoingCallAction(action)
    }

    private fun createNavigationBarStyle(): SystemBarStyle {
        return SystemBarStyle.light(
            scrim = Color.TRANSPARENT,
            darkScrim = Color.TRANSPARENT
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            viewModel.onCallFinishedBySystem(DisconnectCause(DisconnectCause.LOCAL))
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
                CallAction.Answer(
                    displayName = action.call.displayName,
                    phone = action.call.phone,
                    roomAlias = action.call.roomAlias
                ),
            )
            sendBroadcast(callIntent)
        }
    }

    private fun handleOutgoingCallAction(action: CallActivityAction?) {
        if (action is CallActivityAction.OutgoingCall) {
            val callIntent = Intent(applicationContext, CallBroadcast::class.java)
            callIntent.putExtra(
                CallConstants.EXTRA_ACTION,
                CallAction.PlaceOutgoingCall(
                    displayName = action.callerName,
                    phone = action.phoneNumber,
                    roomAlias = action.roomAlias
                ),
            )
            sendBroadcast(callIntent)
        }
    }

    private fun setContent() {
        setContent {
            AppTheme {
                ScreenEventsObserver()

                val state = viewModel.state.collectAsStateWithLifecycle()

                CallScreen(
                    state = state,
                    onCallFinished = ::finishAndRemoveTask,
                    onCallAction = { callAction ->
                        val callIntent = Intent(applicationContext, CallBroadcast::class.java)
                        callIntent.putExtra(
                            CallConstants.EXTRA_ACTION,
                            callAction,
                        )
                        sendBroadcast(callIntent)
                    }
                )
            }
        }
    }

    @Composable
    private fun ScreenEventsObserver() {
        LaunchedEffect(viewModel.events) {
            viewModel.events.collect { event ->
                when (event) {
                    is ScreenEvent.CreateContact -> {
                        openContacts(event.callerName, event.phone)
                    }
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
