package space.livedigital.example

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sequenia.permissionchecker.check
import com.sequenia.permissionchecker.registerPermissionChecker
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.sdk.data.entities.MediaLabel

// To check example, you can use link in browser:
// https://edu.livedigital.space/room/q3_5V3uwik
internal class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private val permissionChecker = registerPermissionChecker()
    private var chooseAudioDeviceAlertDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        checkPostNotificationPermission(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    observeEvents()
                }
            }
        }
        setContent {
            val state by viewModel.state.collectAsState()
            AppTheme {
                MainScreen(
                    state = state,
                    onRestartClicked = { viewModel.onRestartButtonClicked() },
                    onCameraClicked = {
                        checkPermission(
                            listOf(Manifest.permission.CAMERA),
                            viewModel::onCameraButtonClicked
                        )
                    },
                    onMicrophoneClicked = {
                        checkPermission(
                            listOf(Manifest.permission.RECORD_AUDIO),
                            viewModel::onMicrophoneButtonClicked
                        )
                    },
                    onFlipCameraClick = viewModel::onFlipCameraButtonClicked,
                    onAudioDeviceClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            listOf(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            emptyList()
                        }

                        checkPermission(permissions, viewModel::onAudioDeviceChooseRequired)
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
        chooseAudioDeviceAlertDialog?.listView?.let { alertDialogListView ->
            (alertDialogListView.adapter as? ArrayAdapter<String>)?.apply { notifyDataSetChanged() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) stopService(Intent(this, CallService::class.java))
    }

    private fun showAudioDeviceChooseAlertDialog() {
        viewModel.onAudioDeviceChooseRequired()
    }

    private fun checkPermission(
        permissions: List<String>,
        permissionGrantedAction: () -> Unit,
        permissionDeniedAction: (() -> Unit)? = null,
    ) {
        permissionChecker.check(permissions.toTypedArray()) {
            onAllGranted(permissionGrantedAction)

            onAnyDenied { permissionDeniedAction?.invoke() }
        }
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

    private fun checkPostNotificationPermission(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(
                    listOf(Manifest.permission.POST_NOTIFICATIONS),
                    viewModel::onPostNotificationsPermissionGranted
                )
            } else {
                viewModel.onPostNotificationsPermissionGranted()
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