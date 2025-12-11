package space.livedigital.example

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
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
        observeState()
    }

    override fun onResume() {
        super.onResume()
        chooseAudioDeviceAlertDialog?.listView?.let { alertDialogListView ->
            (alertDialogListView.adapter as? ArrayAdapter<String>)?.apply { notifyDataSetChanged() }
        }
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

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter?.updateItemsWithDiffUtil(state.remotePeers)
                    handleCameraSwitchButtonState(state)
                    handleLocalVideoState(state)
                    handleLocalAudioState(state)
                    handleChooseAudioDeviceAlertDialog(state)
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
}
