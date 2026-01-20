package space.livedigital.example

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import space.livedigital.example.bson.BSONObjectIdGenerator
import space.livedigital.example.entities.MoodhoodParticipant
import space.livedigital.example.entities.PeerAppData
import space.livedigital.example.entities.Room
import space.livedigital.example.entities.SignalingToken
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
import space.livedigital.sdk.data.entities.DominantSpeaker
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
import kotlin.time.TimeMark
import kotlin.time.TimeSource.Monotonic.markNow

class MainViewModel() : ViewModel(), KoinComponent {

    val state
        get() = mutableState
    val eventFlow
        get() = eventChannel.receiveAsFlow()

    private val mutableState = MutableStateFlow(ScreenState())
    private val eventChannel = Channel<ScreenEvent>(Channel.UNLIMITED)
    private val apiClient = MoodHoodApiClient(MOODHOOD_API_URL)
    private val availableRoutesChangedDelegate = createAvailableRoutesChangedDelegate()
    private var localParticipantId: String? = null
    private var session: ChannelSession? = null
    private var liveDigitalEngine: LiveDigitalEngine? = null
    private var isLocalVideoPaused = false

    // Need to save reference of delegate (because in sdk delegate is Weak Reference)

    fun onPostNotificationsPermissionGranted() {
        viewModelScope.launch {
            startConference()
            eventChannel.send(ScreenEvent.ShowCallNotification)
        }
    }

    fun onAppBecameFocused() {
        if (session != null && isLocalVideoPaused) {
            startLocalVideo()
            isLocalVideoPaused = false
        }
    }

    fun onAppBecameUnfocused() {
        if (session != null && mutableState.value.isLocalVideoOn) {
            isLocalVideoPaused = true
            stopLocalVideo()
        }
    }

    fun onRestartButtonClicked() {
        restartSession()
    }

    fun onFlipCameraButtonClicked() {
        liveDigitalEngine?.cameraManager?.flipCamera()
    }

    fun onCameraButtonClicked() {
        if (mutableState.value.isLocalVideoOn) stopLocalVideo() else startLocalVideo()
    }

    fun onMicrophoneButtonClicked() {
        if (mutableState.value.isLocalAudioOn) stopLocalAudio() else startLocalAudio()
    }

    fun onAudioDeviceChooseRequired() {
        mutableState.update {
            mutableState.value.copy(isAudioDevicePickerShown = true)
        }
    }

    fun onAudioDeviceChooseDismissed() {
        mutableState.update {
            mutableState.value.copy(isAudioDevicePickerShown = false)
        }
    }

    fun onAudioDeviceSelected(deviceName: String) {
        val actualAvailableAudioDevices =
            liveDigitalEngine?.audioRouter?.getAvailableRoutes().orEmpty()
        val audioRoute = actualAvailableAudioDevices.find { it.kind.name == deviceName }
            ?: return

        liveDigitalEngine?.audioRouter?.updateCurrentRoute(
            audioRoute,
            object : UpdateCurrentRouteCallback {
                override fun onCurrentRouteUpdated(audioRoute: AudioRoute) {}
                override fun onRouteAlreadyCurrent() {}
                override fun onCurrentRouteUpdateError() {}
            }
        )
    }

    private fun createAvailableRoutesChangedDelegate(): AudioRouter.AvailableRoutesChangedDelegate =
        object : AudioRouter.AvailableRoutesChangedDelegate {
            override fun availableRoutesChanged(availableRoutes: List<AudioRoute>) {
                mutableState.update {
                    mutableState.value.copy(
                        availableAudioDeviceNames = availableRoutes.map { it.kind.name },
                        audioDeviceIndex = availableRoutes.indexOfFirst { it.isCurrent }
                    )
                }
            }
        }

    private suspend fun startConference() {
        authorize()

        val room = getRoom() ?: return
        val channelId = room.channelId
        if (channelId == null) {
            Log.e(TAG, "Error getting channelId from: $room")
            return
        }

        val participant = createParticipant() ?: return
        val participantId = participant.id
        localParticipantId = participantId
        if (participantId == null) {
            Log.e(TAG, "Error getting participantId from: $participant")
            return
        }

        val signalingTokenResult = getSignalingToken(participantId)
        val signalingToken = signalingTokenResult?.signalingToken
        if (signalingToken == null) {
            Log.e(TAG, "Error getting signalingToken from: $signalingTokenResult")
            return
        }

        liveDigitalEngine = get<LiveDigitalEngine>()

        initDelegates()

        connectToChannel(channelId, participantId, signalingToken)
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

    private fun initDelegates() {
        liveDigitalEngine?.cameraManager?.delegate = object : CameraManagerDelegate {
            override fun cameraManagerSwitchedCamera(
                cameraManager: CameraManager,
                cameraPosition: CameraPosition
            ) {
                mutableState.update {
                    mutableState.value.copy(localCameraPosition = cameraPosition)
                }
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
        Log.d(TAG, "connect to channel")
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
            successAction = {
                session = it
            }
        )
    }

    private fun createChannelSessionDelegate(): ChannelSessionDelegate {
        return object : ChannelSessionDelegate {
            override fun peersJoined(peers: List<Peer>) {
                for (peer in peers) {
                    if (peer.id == session?.myPeerId) {
                        // TODO: On demand, modify sdk to be able to show your remote peer
                        break
                    }

                    Log.d(TAG, "Peer ${peer.id} joined")
                    val peerWithUpdateTime = PeerWithUpdateTime(
                        peer = peer,
                        updateTimeMark = markNow()
                    )
                    mutableState.update {
                        mutableState.value.copy(remotePeers = it.remotePeers + peerWithUpdateTime)
                    }
                }
            }

            override fun peerDisconnected(peerId: PeerId) {
                Log.d(TAG, "Peer $peerId disconnected")
                mutableState.update {
                    val peers = mutableState.value.remotePeers.toMutableList()
                    peers.removeIf { it.peer.id == peerId }
                    mutableState.value.copy(remotePeers = peers.toList())
                }
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
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                            it.peer.id == peer.id
                        })
                }
            }

            override fun peerCanPauseVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can pause video $label")
                session?.suspendVideo(peer, label)
            }

            override fun peerPausedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} paused video $label")
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(
                            peerWithUpdateTime
                        ) {
                            it.peer.id == peer.id
                        })
                }
            }

            override fun peerStoppedVideo(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} stopped video $label")
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                            it.peer.id == peer.id
                        }
                    )
                }
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
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                            it.peer.id == peer.id
                        }
                    )
                }
            }

            override fun peerCanPauseAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} can pause audio $label")
                session?.suspendAudio(peer, label)
            }

            override fun peerPausedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} paused audio $label")
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                            it.peer.id == peer.id
                        }
                    )
                }
            }

            override fun peerStoppedAudio(peer: Peer, label: MediaLabel) {
                Log.d(TAG, "Peer ${peer.id} stopped audio $label")
                val peerWithUpdateTime = PeerWithUpdateTime(peer = peer, updateTimeMark = markNow())
                mutableState.update {
                    mutableState.value.copy(
                        remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                            it.peer.id == peer.id
                        }
                    )
                }
            }

            override fun peerAppDataUpdated(peerId: PeerId, appData: JSONObject) {}

            override fun peerPermissionsUpdated(peerId: PeerId, permissions: List<MediaLabel>) {}

            override fun stoppedLocalVideo(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun stoppedLocalAudio(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun forceStoppedLocalMedia(label: MediaLabel) {}

            override fun connectedToChannel() {
                viewModelScope.launch {
                    joinRoom()
                }
            }

            override fun reconnectedToChannel() {}

            override fun disconnectedFromChannel() {}

            override fun onStatusChanged(status: ChannelSessionStatus) {
                when (status) {
                    ChannelSessionStatus.RESTARTING -> {}
                    ChannelSessionStatus.STARTING -> {}
                    ChannelSessionStatus.STARTED -> {}
                    ChannelSessionStatus.STOPPED -> restartSession()
                    ChannelSessionStatus.STOPPING -> {}
                }
            }

            override fun onChannelErrorOccurred(error: ChannelError) {}

            override fun onCustomEventReceived(event: CustomEvent) {}

            override fun activityConfirmationRequired(data: ActivityConfirmationData) {}

            override fun activityConfirmationExpired() {}

            override fun activityConfirmationAcquired() {}

            override fun peerVolumesUpdated(volumes: List<PeerVolume>) {}

            override fun silenceHasBeenSet() {}
            override fun gotDominantSpeaker(dominantSpeaker: DominantSpeaker) {}

            override fun gotChannelStateConsistencyIssues(
                issues: Set<ChannelStateConsistencyIssue>
            ) {
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

    private fun startLocalVideo() {
        val localVideoSource = liveDigitalEngine?.startCameraVideoSource(
            videoOutputFormat = null,
            videoEncodingPresets = null,
            visualEffects = listOf(),
            videoSourceType = VideoSourceType.CAMERA
        )
        localVideoSource?.let { source ->
            mutableState.update {
                mutableState.value.copy(
                    localVideoSource = source,
                    isLocalVideoOn = true
                )
            }
        }
    }

    private fun stopLocalVideo() {
        if (!mutableState.value.isLocalVideoOn) {
            mutableState.update {
                mutableState.value.copy(localVideoSource = null)
            }
            return
        }

        mutableState.value.localVideoSource?.let {
            liveDigitalEngine?.stopMediaSource(
                mediaSource = it,
                videoSourceType = VideoSourceType.CAMERA
            )

            mutableState.update {
                mutableState.value.copy(isLocalVideoOn = false)
            }
        }
    }

    private fun startLocalAudio() {
        val localAudioSource = liveDigitalEngine?.startAudioSource(audioEncodingPresets = null)
        localAudioSource?.let { source ->
            mutableState.update {
                mutableState.value.copy(
                    localAudioSource = source,
                    isLocalAudioOn = true
                )
            }
        }
    }

    private fun stopLocalAudio() {
        if (!mutableState.value.isLocalAudioOn) {
            mutableState.update {
                mutableState.value.copy(localAudioSource = null)
            }
            return
        }

        mutableState.value.localAudioSource?.let {
            liveDigitalEngine?.stopMediaSource(mediaSource = it, videoSourceType = null)
            mutableState.update {
                mutableState.value.copy(isLocalAudioOn = false)
            }
        }
    }

    private fun restartSession() {
        liveDigitalEngine?.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {
                viewModelScope.launch {
                    stopLocalVideo()
                    stopLocalAudio()
                    session = null
                    apiClient.logout()
                    startConference()
                }
            }
        })
    }

    companion object {
        const val MOODHOOD_API_URL = "https://moodhood-api.livedigital.space/"
        const val MOODHOOD_CLIENT_ID = "moodhood-demo"
        const val MOODHOOD_CLIENT_SECRET = "demo12345abcde6789zxcvDemo"
        const val CLIENT_CREDENTIALS_GARANT_TYPE = "client_credentials"

        const val TEST_SPACE_ID = "612dbb98b2f9d4a99f18f553"
        const val TEST_ROOM_ID = "61554214ae218a31f78e8bc8"

        const val TAG = "LivedigitalAndroidSdkExample"
    }
}

private fun <T> List<T>.replaceBy(item: T, predicate: (T) -> Boolean): List<T> =
    (filterNot(predicate) + item)

data class ScreenState(
    val remotePeers: List<PeerWithUpdateTime> = emptyList(),
    val isLocalVideoOn: Boolean = false,
    val isLocalAudioOn: Boolean = false,
    val localVideoSource: VideoSource? = null,
    val localAudioSource: AudioSource? = null,
    val isAudioDevicePickerShown: Boolean = false,
    val availableAudioDeviceNames: List<String> = emptyList(),
    val audioDeviceIndex: Int = 0,
    val localCameraPosition: CameraPosition = CameraPosition.BACK
)

sealed interface ScreenEvent {

    data object ShowCallNotification : ScreenEvent
}

data class PeerWithUpdateTime(val peer: Peer, val updateTimeMark: TimeMark)