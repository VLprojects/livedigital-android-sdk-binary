package space.livedigital.example.calls

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.DisconnectCause
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
import space.livedigital.example.calls.constants.CallConstants
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.internal.broadcasts.CallBroadcast
import space.livedigital.example.calls.internal.service.CallService

class CallActivity : ComponentActivity() {

    private val viewModel by viewModel<CallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCallActivity()

        val action = with(intent) {
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

        if (action is CallAction.Answer) {
            val callIntent = Intent(applicationContext, CallBroadcast::class.java)
            callIntent.putExtra(
                CallConstants.EXTRA_ACTION,
                action,
            )
            sendBroadcast(callIntent)
            val intent = Intent(this, CallService::class.java).apply {
                this.action = CallConstants.ACTION_UPDATE_CALL
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
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            viewModel.onCallEnded(DisconnectCause(DisconnectCause.LOCAL))
            stopService(Intent(this, CallService::class.java))
        }
    }

    private fun setupCallActivity() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguardManager = getSystemService<KeyguardManager>()
        keyguardManager?.requestDismissKeyguard(this, null)
    }
}