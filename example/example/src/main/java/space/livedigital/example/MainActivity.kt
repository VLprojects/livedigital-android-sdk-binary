package space.livedigital.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.sdk.data.entities.MediaLabel

// To check example, you can use link in browser:
// https://edu.livedigital.space/room/q3_5V3uwik
internal class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        setContent {
            val state by viewModel.state.collectAsState()

            var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }

                if (allGranted) {
                    pendingAction?.invoke()
                }
                pendingAction = null
            }

            val requestPermission = remember {
                { permissions: List<String>, onGranted: () -> Unit ->
                    val missingPermissions = permissions.filter {
                        checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missingPermissions.isEmpty()) {
                        onGranted()
                    } else {
                        pendingAction = onGranted
                        permissionLauncher.launch(missingPermissions.toTypedArray())
                    }
                }
            }

            val scope = rememberCoroutineScope()

            LifecycleStartEffect(Unit) {
                val eventsJob = scope.launch {
                    observeEvents()
                }

                onStopOrDispose {
                    eventsJob.cancel()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermission(listOf(Manifest.permission.POST_NOTIFICATIONS)) {
                        viewModel.onPostNotificationsPermissionGranted()
                    }
                } else {
                    viewModel.onPostNotificationsPermissionGranted()
                }
            }

            AppTheme {
                MainScreen(
                    state = state,
                    onRestartClicked = { viewModel.onRestartButtonClicked() },
                    onCameraClicked = {
                        requestPermission(listOf(Manifest.permission.CAMERA)) {
                            viewModel.onCameraButtonClicked()
                        }
                    },
                    onMicrophoneClicked = {
                        requestPermission(listOf(Manifest.permission.RECORD_AUDIO)) {
                            viewModel.onMicrophoneButtonClicked()
                        }
                    },
                    onFlipCameraClick = viewModel::onFlipCameraButtonClicked,
                    onAudioDeviceClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            listOf(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            emptyList()
                        }
                        requestPermission(permissions) {
                            viewModel.onAudioDeviceChooseRequired()
                        }
                    },
                    onAudioDeviceSelected = viewModel::onAudioDeviceSelected,
                    onAudioDeviceDismiss = viewModel::onAudioDeviceChooseDismissed
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isChangingConfigurations.not()) viewModel.onAppBecameUnfocused()
    }


    override fun onResume() {
        super.onResume()
        viewModel.onAppBecameFocused()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) stopService(Intent(this, CallService::class.java))
    }

    private suspend fun observeEvents() {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ScreenEvent.ShowCallNotification -> {
                    val serviceIntent =
                        Intent(this.applicationContext, CallService::class.java).apply {
                            setPackage(this@MainActivity.packageName)
                        }
                    startForegroundService(serviceIntent)
                }
            }
        }
    }
}

data class PeerWithMediaContentType(
    val peerWithUpdateTime: PeerWithUpdateTime,
    val videoContentType: PeerVideoContentType
)

enum class PeerVideoContentType(val videoMediaLabel: MediaLabel) {
    CUSTOM_VIDEO(MediaLabel.CUSTOM_VIDEO),
    SCREEN_VIDEO(MediaLabel.SCREEN_VIDEO),
    CAMERA(MediaLabel.CAMERA)
}