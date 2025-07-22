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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sequenia.permissionchecker.check
import com.sequenia.permissionchecker.registerPermissionChecker
import kotlinx.coroutines.launch
import org.json.JSONObject
import space.livedigital.example.bson.BSONObjectIdGenerator
import space.livedigital.example.databinding.ActivityMainBinding
import space.livedigital.example.entities.MoodhoodParticipant
import space.livedigital.example.entities.PeerAppData
import space.livedigital.example.entities.Room
import space.livedigital.example.entities.SignalingToken
import space.livedigital.example.logger.ConsoleLogger
import space.livedigital.example.moodhood_api.MoodHoodApiClient
import space.livedigital.example.moodhood_api.result.api.ExecutionError
import space.livedigital.example.moodhood_api.result.api.ExecutionResult
import space.livedigital.example.utils.JsonUtils
import space.livedigital.sdk.channel.ChannelError
import space.livedigital.sdk.channel.ChannelId
import space.livedigital.sdk.channel.ChannelSession
import space.livedigital.sdk.channel.ChannelSessionDelegate
import space.livedigital.sdk.channel.ChannelSessionStatus
import space.livedigital.sdk.data.entities.ActivityConfirmationData
import space.livedigital.sdk.data.entities.CustomEvent
import space.livedigital.sdk.data.entities.MediaLabel
import space.livedigital.sdk.data.entities.Peer
import space.livedigital.sdk.data.entities.PeerId
import space.livedigital.sdk.data.entities.PeerVolume
import space.livedigital.sdk.data.entities.Role
import space.livedigital.sdk.data.entities.StockChannelSessionParams
import space.livedigital.sdk.data.entities.channel_state_consistency.ChannelStateConsistencyIssue
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.LiveDigitalEngineDelegate
import space.livedigital.sdk.engine.LiveDigitalEngineDestroyDelegate
import space.livedigital.sdk.engine.LiveDigitalEngineError
import space.livedigital.sdk.engine.StockLiveDigitalEngine
import space.livedigital.sdk.media.MediaSourceId
import space.livedigital.sdk.media.audio.AudioRoute
import space.livedigital.sdk.media.audio.AudioRouter
import space.livedigital.sdk.media.audio.AudioRouter.UpdateCurrentRouteCallback
import space.livedigital.sdk.media.audio.AudioSource
import space.livedigital.sdk.media.video.CameraManager
import space.livedigital.sdk.media.video.CameraManagerDelegate
import space.livedigital.sdk.media.video.CameraPosition
import space.livedigital.sdk.media.video.VideoSource
import space.livedigital.sdk.media.video.visual_effects.VideoSourceType

internal class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    private val apiClient = MoodHoodApiClient(MOODHOOD_API_URL)

    private var liveDigitalEngine: LiveDigitalEngine? = null
    private var session: ChannelSession? = null

    private var localParticipantId: String? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null

    private val permissionChecker = registerPermissionChecker()

    private var adapter: RemotePeerAdapter? = null

    private var chooseAudioDeviceAlertDialog: AlertDialog? = null

    // Need to save reference of delegate (because in sdk delegate is Weak Reference)
    private val availableRoutesChangedDelegate = createAvailableRoutesChangedDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        initPeerList()
        initListeners()
        startConference()
    }

    override fun onResume() {
        super.onResume()
        chooseAudioDeviceAlertDialog?.listView?.let { alertDialogListView ->
            (alertDialogListView.adapter as? ArrayAdapter<String>)?.apply { notifyDataSetChanged() }
        }
    }

    private fun createAvailableRoutesChangedDelegate(): AudioRouter.AvailableRoutesChangedDelegate =
        object : AudioRouter.AvailableRoutesChangedDelegate {
            override fun availableRoutesChanged(availableRoutes: List<AudioRoute>) {
                chooseAudioDeviceAlertDialog?.listView?.let { alertDialogListView ->
                    val availableAudioDeviceNames = availableRoutes.map { it.kind.name }
                    val selectedAudioDeviceIndex = availableRoutes.indexOfFirst { it.isCurrent }

                    (alertDialogListView.adapter as? ArrayAdapter<String>)?.apply {
                        clear()
                        addAll(availableAudioDeviceNames)
                        notifyDataSetChanged()
                    }
                    alertDialogListView.setItemChecked(selectedAudioDeviceIndex, true)
                }
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
            restartConference()
        }

        binding?.cameraStatusEnableButton?.setOnClickListener {
            val cameraPermissionGrantedAction = {
                if (localVideoSource == null) startLocalVideo() else stopLocalVideo()
            }

            checkPermission(
                listOf(Manifest.permission.CAMERA),
                cameraPermissionGrantedAction
            )
        }

        binding?.micStatusEnableButton?.setOnClickListener {
            val microphonePermissionGrantedAction = {
                if (localAudioSource == null) startLocalAudio() else stopLocalAudio()
            }

            checkPermission(
                listOf(Manifest.permission.RECORD_AUDIO),
                microphonePermissionGrantedAction
            )
        }

        binding?.cameraSwitchButton?.setOnClickListener {
            liveDigitalEngine?.cameraManager?.flipCamera()
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

    private fun startLocalVideo() {
        val peerView = binding?.localPeerView ?: return

        localVideoSource = liveDigitalEngine?.startCameraVideoSource(
            videoOutputFormat = null,
            videoEncodingPresets = null,
            visualEffects = listOf(),
            videoSourceType = VideoSourceType.CAMERA
        )
        localVideoSource?.let { source ->
            peerView.renderVideoSource(source)
            binding?.cameraStatusEnableButton?.setIcon(R.drawable.ic_baseline_videocam_24)
        }
    }

    private fun stopLocalVideo() {
        val peerView = binding?.localPeerView ?: return

        localVideoSource?.let {
            liveDigitalEngine?.stopMediaSource(
                mediaSource = it,
                videoSourceType = VideoSourceType.CAMERA
            )
            peerView.stopRenderingVideoSource(videoSource = it)
            binding?.cameraStatusEnableButton?.setIcon(R.drawable.ic_baseline_videocam_off_24)
        }

        localVideoSource = null
    }

    private fun startLocalAudio() {
        localAudioSource = liveDigitalEngine?.startAudioSource(audioEncodingPresets = null)
        localAudioSource?.let {
            binding?.micStatusEnableButton?.setIcon(R.drawable.ic_baseline_mic_24)
        }
    }

    private fun stopLocalAudio() {
        localAudioSource?.let {
            liveDigitalEngine?.stopMediaSource(mediaSource = it, videoSourceType = null)
            binding?.micStatusEnableButton?.setIcon(R.drawable.ic_baseline_mic_off_24)
        }

        localAudioSource = null
    }

    private fun showAudioDeviceChooseAlertDialog() {
        val availableAudioDevices =
            liveDigitalEngine?.audioRouter?.getAvailableRoutes().orEmpty()
        val availableAudioDeviceNames = availableAudioDevices.map { it.kind.name }
        val selectedAudioDeviceIndex = availableAudioDevices.indexOfFirst { it.isCurrent }

        chooseAudioDeviceAlertDialog = showSimpleSelectorDialog(
            context = this,
            title = "Choose audio device",
            items = availableAudioDeviceNames,
            initialItemIndex = selectedAudioDeviceIndex
        ) { index, value ->
            val actualAvailableAudioDevices =
                liveDigitalEngine?.audioRouter?.getAvailableRoutes().orEmpty()
            val audioRoute = actualAvailableAudioDevices.find { it.kind.name == value }
                ?: return@showSimpleSelectorDialog

            liveDigitalEngine?.audioRouter?.updateCurrentRoute(
                audioRoute,
                object : UpdateCurrentRouteCallback {
                    override fun onCurrentRouteUpdated(audioRoute: AudioRoute) {}
                    override fun onRouteAlreadyCurrent() {}
                    override fun onCurrentRouteUpdateError() {}
                }
            )
        }.apply {
            setOnDismissListener {
                chooseAudioDeviceAlertDialog = null
            }
        }
    }

    private fun startConference() {
        lifecycleScope.launch {
            authorize()

            val room = getRoom() ?: return@launch
            val channelId = room.channelId
            if (channelId == null) {
                Log.e(TAG, "Error getting channelId from: $room")
                return@launch
            }

            val participant = createParticipant() ?: return@launch
            val participantId = participant.id
            localParticipantId = participantId
            if (participantId == null) {
                Log.e(TAG, "Error getting participantId from: $participant")
                return@launch
            }

            val signalingTokenResult = getSignalingToken(participantId)
            val signalingToken = signalingTokenResult?.signalingToken
            if (signalingToken == null) {
                Log.e(TAG, "Error getting signalingToken from: $signalingTokenResult")
                return@launch
            }

            initLiveDigitalEngine()

            connectToChannel(channelId, participantId, signalingToken)
        }
    }

    private fun restartConference() {
        liveDigitalEngine?.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {
                lifecycleScope.launch {
                    stopLocalVideo()
                    stopLocalAudio()
                    session = null
                    adapter?.clear()
                    apiClient.logout()
                    startConference()
                }
            }
        })
    }

    override fun onDestroy() {
        binding = null
        liveDigitalEngine?.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {}
        })
        super.onDestroy()
    }

    private fun initLiveDigitalEngine() {
        liveDigitalEngine =
            StockLiveDigitalEngine(
                context = applicationContext,
                externalLoggers = listOf(ConsoleLogger)
            )

        liveDigitalEngine?.cameraManager?.delegate = object : CameraManagerDelegate {
            override fun cameraManagerSwitchedCamera(
                cameraManager: CameraManager,
                cameraPosition: CameraPosition
            ) {
                val drawableResId = when (cameraPosition) {
                    CameraPosition.BACK -> R.drawable.ic_outline_photo_camera_back_24
                    CameraPosition.FRONT -> R.drawable.ic_outline_photo_camera_front_24
                }
                binding?.cameraSwitchButton?.setIcon(drawableResId)
            }
        }

        liveDigitalEngine?.delegate = object : LiveDigitalEngineDelegate {
            override fun engineFailed(error: LiveDigitalEngineError) {
                Log.e(TAG, "Engine failed $error")
            }
        }

        liveDigitalEngine?.audioRouter?.setAvailableRoutesChangedDelegate(
            availableRoutesChangedDelegate
        )
    }

    private fun connectToChannel(
        channelId: String,
        participantId: String,
        signalingToken: String
    ) {
        val appData = PeerAppData(
            name = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        val appDataJson = JsonUtils.encodeToJsonString(appData)

        val channelSessionParams = StockChannelSessionParams(
            channelId = ChannelId(channelId),
            participantId = participantId,
            role = Role.HOST,
            signalingToken = signalingToken,
            peerId = PeerId(participantId),
            appData = JSONObject(appDataJson),
            analyticsMetaKeyValues = emptyMap()
        )

        liveDigitalEngine?.connectToChannel(
            channelSessionParams = channelSessionParams,
            delegate = createChannelSessionDelegate(),
            successAction = { session = it }
        )
    }

    private fun createChannelSessionDelegate(): ChannelSessionDelegate {
        return object : ChannelSessionDelegate {
            override fun peerJoined(peer: Peer) {
                if (peer.id == session?.myPeerId) {
                    // TODO: On demand, modify sdk to be able to show your remote peer
                    return
                }

                Log.d(TAG, "Peer ${peer.id} joined")
                adapter?.addPeer(peer)
            }

            override fun peerDisconnected(peerId: PeerId) {
                Log.d(TAG, "Peer $peerId disconnected")
                adapter?.removePeer(peerId)
            }

            override fun peerCanStartVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can start video $label")
                session?.startVideo(peer, label)
            }

            override fun peerStartedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} started video $label")

                //Redundant functionality. Should it be removed.
                peer.setIsVideoConsumerCanBeImmediatelyResumed(label, true)

                session?.proceedVideo(peer, label)
            }

            override fun peerCanResumeVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can resume video $label")
                session?.proceedVideo(peer, label)
            }

            override fun peerResumedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} resumed video $label")
                adapter?.updatePeer(peer)
            }

            override fun peerCanPauseVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can pause video $label")
                session?.suspendVideo(peer, label)
            }

            override fun peerPausedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} paused video $label")
                adapter?.updatePeer(peer)
            }

            override fun peerStoppedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} stopped video $label")
                adapter?.updatePeer(peer)
            }

            override fun peerCanStartAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can start audio $label")
                session?.startAudio(peer, label)
            }

            override fun peerStartedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} started audio $label")
            }

            override fun peerCanResumeAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can resumed audio $label")
                session?.proceedAudio(peer, label)
            }

            override fun peerResumedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} resumed audio $label")
                adapter?.updatePeer(peer)
            }

            override fun peerCanPauseAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can pause audio $label")
                session?.suspendAudio(peer, label)
            }

            override fun peerPausedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} paused audio $label")
                adapter?.updatePeer(peer)
            }

            override fun peerStoppedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} stopped audio $label")
                adapter?.updatePeer(peer)
            }

            override fun peerAppDataUpdated(peerId: PeerId, appData: JSONObject) {}

            override fun peerPermissionsUpdated(peerId: PeerId, permissions: List<MediaLabel>) {}

            override fun stoppedLocalVideo(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun stoppedLocalAudio(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun forceStoppedLocalMedia(label: MediaLabel) {}

            override fun connectedToChannel() {
                lifecycleScope.launch {
                    joinRoom()
                }
            }

            override fun reconnectedToChannel() {}

            override fun disconnectedFromChannel() {}

            override fun onStatusChanged(status: ChannelSessionStatus) {}

            override fun onChannelErrorOccurred(error: ChannelError) {}

            override fun onCustomEventReceived(event: CustomEvent) {}

            override fun activityConfirmationRequired(data: ActivityConfirmationData) {}

            override fun activityConfirmationExpired() {}

            override fun activityConfirmationAcquired() {}

            override fun peerVolumesUpdated(volumes: List<PeerVolume>) {}

            override fun silenceHasBeenSet() {}

            override fun gotChannelStateConsistencyIssues(
                issues: Set<ChannelStateConsistencyIssue>
            ) {}
        }
    }

    private suspend fun authorize() {
        val userTokenResult = apiClient.authorizeAsGuest(
            MOODHOOD_CLIENT_ID,
            MOODHOOD_CLIENT_SECRET,
            CLIENT_CREDENTIALS_GARANT_TYPE
        )
        when (userTokenResult) {
            is ExecutionResult.Success -> {
                Log.i(TAG, "Created user token")
            }

            is ExecutionResult.Error -> {
                val error = userTokenResult.error
                val message = when (error) {
                    is ExecutionError.Expected -> error.data.message
                    is ExecutionError.Failure -> error.throwable.message

                }
                Log.e(TAG, "Error creating user token: $message")
            }
        }
    }

    private suspend fun getRoom(): Room? {
        val roomResult = apiClient.getRoom(TEST_SPACE_ID, TEST_ROOM_ID)
        return when (roomResult) {
            is ExecutionResult.Success -> {
                Log.i(TAG, "Room details received: ${roomResult.data}")
                roomResult.data
            }

            is ExecutionResult.Error -> {
                val error = roomResult.error
                val message = when (error) {
                    is ExecutionError.Expected -> error.data.message
                    is ExecutionError.Failure -> error.throwable.message

                }
                Log.e(TAG, "Error getting room details: $message")
                null
            }
        }
    }

    private suspend fun createParticipant(): MoodhoodParticipant? {
        val participantResult = apiClient.createParticipant(
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            role = "host",
            clientUniqueId = BSONObjectIdGenerator.generateBSONObjectId(),
            spaceId = TEST_SPACE_ID,
            roomId = TEST_ROOM_ID
        )

        return when (participantResult) {
            is ExecutionResult.Success -> {
                Log.i(TAG, "Created participant: ${participantResult.data}")
                participantResult.data
            }

            is ExecutionResult.Error -> {
                val error = participantResult.error
                val message = when (error) {
                    is ExecutionError.Expected -> error.data.message
                    is ExecutionError.Failure -> error.throwable.message

                }
                Log.e(TAG, "Error creating participant: $message")
                null
            }
        }
    }

    private suspend fun getSignalingToken(participantId: String): SignalingToken? {
        val signalingTokenResult = apiClient.getSignalingToken(TEST_SPACE_ID, participantId)
        return when (signalingTokenResult) {
            is ExecutionResult.Success -> {
                Log.i(TAG, "Created signaling token")
                signalingTokenResult.data
            }

            is ExecutionResult.Error -> {
                val error = signalingTokenResult.error
                val message = when (error) {
                    is ExecutionError.Expected -> error.data.message
                    is ExecutionError.Failure -> error.throwable.message

                }
                Log.e(TAG, "Error creating signaling token: $message")
                null
            }
        }
    }

    private suspend fun joinRoom() {
        val participantId = localParticipantId
        if (participantId == null) {
            Log.e(TAG, "Failed to join room: missing participantId")
            return
        }

        val joinRoomResult = apiClient.joinRoom(participantId, TEST_SPACE_ID, TEST_ROOM_ID)
        when (joinRoomResult) {
            is ExecutionResult.Success -> {
                Log.i(TAG, "Joined room")
            }

            is ExecutionResult.Error -> {
                val error = joinRoomResult.error
                val message = when (error) {
                    is ExecutionError.Expected -> error.data.message
                    is ExecutionError.Failure -> error.throwable.message

                }
                Log.e(TAG, "Failed to join room: $message")
            }
        }
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

    private fun TextView.setIcon(drawableResId: Int) {
        val drawable = ResourcesCompat.getDrawable(resources, drawableResId, theme)
        setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }

    private fun showSimpleSelectorDialog(
        context: Context,
        title: String,
        items: List<String>,
        initialItemIndex: Int,
        onSelected: (index: Int, value: String) -> Unit
    ): AlertDialog {
        var selectedItemIndex = initialItemIndex
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            items.toMutableList()
        )

        return AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(adapter, selectedItemIndex) { _, newSelectedItemIndex ->
                selectedItemIndex = newSelectedItemIndex
            }
            .setPositiveButton("OK") { dialog, _ ->
                val selectedItem = adapter?.getItem(selectedItemIndex) ?: return@setPositiveButton
                onSelected(selectedItemIndex, selectedItem)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private companion object {
        // To check example, you can use link in browser:
        // https://edu.livedigital.space/room/LpT7NITj6C

        const val MOODHOOD_API_URL = "https://moodhood-api.livedigital.space/"
        const val MOODHOOD_CLIENT_ID = "moodhood-demo"
        const val MOODHOOD_CLIENT_SECRET = "demo12345abcde6789zxcvDemo"
        const val CLIENT_CREDENTIALS_GARANT_TYPE = "client_credentials"

        const val TEST_SPACE_ID = "6144806aac512ee6cd29be10"
        const val TEST_ROOM_ID = "679cbb21da09394f6df29bda"

        const val TAG = "LivedigitalAndroidSdkExample"
    }
}
