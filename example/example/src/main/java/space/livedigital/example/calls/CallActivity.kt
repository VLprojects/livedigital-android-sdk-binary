package space.livedigital.example.calls

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.SessionViewModel
import space.livedigital.example.calls.utils.CallBroadcast
import space.livedigital.example.calls.utils.CallNotificationManager
import space.livedigital.example.calls.utils.CallNotificationManager.Companion.TELECOM_NOTIFICATION_ACTION
import space.livedigital.example.calls.utils.CallService
import space.livedigital.example.telecom_calls.utils.TelecomCallRepository
import kotlin.getValue

class CallActivity : ComponentActivity() {

    private val viewModel by viewModel<SessionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCallActivity()

        val action = with(intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(
                    TELECOM_NOTIFICATION_ACTION,
                    CallAction::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(TELECOM_NOTIFICATION_ACTION)
            }
        }

        if (action is CallAction.Answer) {
            val callIntent = Intent(applicationContext, CallBroadcast::class.java)
            callIntent.putExtra(
                TELECOM_NOTIFICATION_ACTION,
                action,
            )
            sendBroadcast(callIntent)
            val intent = Intent(this, CallService::class.java).apply {
                this.action = CallService.ACTION_UPDATE_CALL
            }
            startForegroundService(intent)
        }

        setContent {
            MaterialTheme {
                val state = viewModel.state.collectAsStateWithLifecycle()

                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    CallScreen(
                        state = state,
                        onCallFinished = {
                            TelecomCallRepository.endCall()
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    private fun setupCallActivity() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguardManager = getSystemService<KeyguardManager>()
        keyguardManager?.requestDismissKeyguard(this, null)
    }
}