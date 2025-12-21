package space.livedigital.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sequenia.permissionchecker.check
import com.sequenia.permissionchecker.registerPermissionChecker
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import space.livedigital.example.databinding.ActivityMainBinding
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.media.video.CameraPosition
import space.livedigital.sdk.view.PeerView
import space.livedigital.sdk.view.VideoRenderer

// To check example, you can use link in browser:
// https://edu.livedigital.space/room/LpT7NITj6C
internal class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private var binding: ActivityMainBinding? = null

    private val permissionChecker = registerPermissionChecker()

    private var adapter: RemotePeerAdapter? = null

    private var chooseAudioDeviceAlertDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        initPeerList()
        initListeners()
        observeStateAndEvents()
        checkPostNotificationPermission(savedInstanceState)
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

    private fun initPeerList() {
        adapter = RemotePeerAdapter(layoutInflater)
        binding?.remotePeerList?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding?.remotePeerList?.adapter = adapter
    }

    private fun initListeners() {
        binding?.restartButton?.setOnClickListener {
            viewModel.onRestartButtonClicked()
        }

        binding?.cameraStatusEnableButton?.setOnClickListener {
            val cameraPermissionGrantedAction = {
                viewModel.onCameraButtonClicked()
            }

            checkPermission(
                listOf(Manifest.permission.CAMERA),
                cameraPermissionGrantedAction
            )
        }

        binding?.micStatusEnableButton?.setOnClickListener {
            val microphonePermissionGrantedAction = {
                viewModel.onMicrophoneButtonClicked()
            }

            checkPermission(
                listOf(Manifest.permission.RECORD_AUDIO),
                microphonePermissionGrantedAction
            )
        }

        binding?.cameraSwitchButton?.setOnClickListener {
            viewModel.onFlipCameraButtonClicked()
        }

        binding?.audioDeviceSwitchButton?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkPermission(
                    listOf(Manifest.permission.BLUETOOTH_CONNECT),
                    permissionGrantedAction = this::showAudioDeviceChooseAlertDialog,
                    permissionDeniedAction = this::showAudioDeviceChooseAlertDialog
                )
            } else {
                showAudioDeviceChooseAlertDialog()
            }
        }
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

    private fun observeStateAndEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    observeState()
                }

                launch {
                    observeEvents()
                }
            }
        }
    }

    private suspend fun observeState() {
        viewModel.state.collect { state ->
            val peers = buildPeers(state)
            adapter?.updateItemsWithDiffUtil(peers)
            handleCameraSwitchButtonState(state)
            handleLocalVideoState(state)
            handleLocalAudioState(state)
            handleChooseAudioDeviceAlertDialog(state)
        }
    }

    private fun buildPeers(state: ScreenState): List<PeerWithMediaContentType> = buildList {
        state.remotePeers.forEach { peer ->
            if (peer.peer.hasMedia(MediaLabel.CUSTOM_VIDEO)) {
                add(
                    PeerWithMediaContentType(
                        peerWithUpdateTime = peer,
                        videoContentType = PeerVideoContentType.CUSTOM_VIDEO
                    )
                )
            }

            if (peer.peer.hasMedia(MediaLabel.SCREEN_VIDEO)) {
                add(
                    PeerWithMediaContentType(
                        peerWithUpdateTime = peer,
                        videoContentType = PeerVideoContentType.SCREEN_VIDEO
                    )
                )
            }

            add(
                PeerWithMediaContentType(
                    peerWithUpdateTime = peer,
                    videoContentType = PeerVideoContentType.CAMERA
                )
            )
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

    private fun handleCameraSwitchButtonState(state: ScreenState) {
        val iconId = when (state.localCameraPosition) {
            CameraPosition.BACK -> R.drawable.ic_outline_photo_camera_back_24
            CameraPosition.FRONT -> R.drawable.ic_outline_photo_camera_front_24
        }

        binding?.cameraSwitchButton?.setIcon(iconId)
    }

    private fun handleLocalVideoState(state: ScreenState) {
        state.localVideoSource?.let { source ->
            if (state.isLocalVideoOn) {
                binding?.localPeerView?.renderVideoSource(source)
                binding?.cameraStatusEnableButton?.setIcon(
                    R.drawable.ic_baseline_videocam_24
                )
                binding?.localPeerView?.setViewScreenVisibilityListener(
                    object : PeerView.ViewScreenVisibilityListener {
                        override fun onViewBecomeVisibleOnScreen(
                            videoRenderer: VideoRenderer
                        ) {
                            binding?.localPeerView?.renderVideoSource(source)
                        }

                        override fun onViewBecomeInvisibleOnScreen(
                            videoRenderer: VideoRenderer
                        ) {
                            binding?.localPeerView?.stopRenderingVideoSource(source)
                        }
                    })
            } else {
                binding?.localPeerView?.stopRenderingVideoSource(videoSource = source)
                binding?.cameraStatusEnableButton?.setIcon(
                    R.drawable.ic_baseline_videocam_off_24
                )
                binding?.localPeerView?.setViewScreenVisibilityListener(null)
            }
        }
    }

    private fun handleLocalAudioState(state: ScreenState) {
        if (state.isLocalAudioOn) {
            binding?.micStatusEnableButton?.setIcon(R.drawable.ic_baseline_mic_24)
        } else {
            binding?.micStatusEnableButton?.setIcon(R.drawable.ic_baseline_mic_off_24)
        }
    }

    private fun handleChooseAudioDeviceAlertDialog(state: ScreenState) {
        when (state.isAudioDevicePickerShown) {
            true -> {
                chooseAudioDeviceAlertDialog?.let {
                    (chooseAudioDeviceAlertDialog?.listView?.apply {
                        (adapter as? ArrayAdapter<String>)?.apply {
                            clear()
                            addAll(state.availableAudioDeviceNames)
                            notifyDataSetChanged()
                        }
                        setItemChecked(state.audioDeviceIndex, true)
                    })
                    return
                }

                chooseAudioDeviceAlertDialog = showSimpleSelectorDialog(
                    context = this@MainActivity,
                    items = state.availableAudioDeviceNames,
                    itemIndex = state.audioDeviceIndex
                ) { _, deviceName ->
                    viewModel.onAudioDeviceSelected(deviceName)
                }.apply {
                    setOnDismissListener {
                        viewModel.onAudioDeviceChooseDismissed()
                    }
                }
            }

            false -> {
                chooseAudioDeviceAlertDialog?.dismiss()
                chooseAudioDeviceAlertDialog = null
            }
        }
    }

    private fun TextView.setIcon(drawableResId: Int) {
        val drawable = ResourcesCompat.getDrawable(resources, drawableResId, theme)
        setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }

    private fun showSimpleSelectorDialog(
        context: Context,
        items: List<String>,
        itemIndex: Int,
        onSelected: (index: Int, value: String) -> Unit
    ): AlertDialog {
        var selectedItemIndex = itemIndex
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            items.toMutableList()
        )

        return AlertDialog.Builder(context)
            .setTitle("Choose audio device")
            .setSingleChoiceItems(adapter, selectedItemIndex) { _, newSelectedItemIndex ->
                selectedItemIndex = newSelectedItemIndex
            }
            .setPositiveButton("OK") { dialog, _ ->
                val selectedItem = adapter.getItem(selectedItemIndex) ?: return@setPositiveButton
                onSelected(selectedItemIndex, selectedItem)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    private fun Peer.hasMedia(label: MediaLabel): Boolean {
        return (this.hasConsumer(label) || this.hasProducerDataInStock(label)) &&
                this.isRemoteProducerResumed(label)
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