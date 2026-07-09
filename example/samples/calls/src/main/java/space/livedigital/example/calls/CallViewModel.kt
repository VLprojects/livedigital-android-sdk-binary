package space.livedigital.example.calls

import android.os.Build
import android.telecom.DisconnectCause
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import space.livedigital.example.calls.backend.ConferenceBackend
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallCommand
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.CallType
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.repositories.ContactsRepository
import space.livedigital.example.calls.repositories.HasContactResult
import space.livedigital.example.entities.PeerAppData
import space.livedigital.example.utils.JsonUtils
import space.livedigital.sdk.channel.ChannelError
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
import space.livedigital.sdk.data.entities.StockChannelSessionParams
import space.livedigital.sdk.data.entities.channel_state_consistency.ChannelStateConsistencyIssue
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.LiveDigitalEngineDelegate
import space.livedigital.sdk.engine.LiveDigitalEngineDestroyDelegate
import space.livedigital.sdk.engine.LiveDigitalEngineError
import space.livedigital.sdk.media.MediaSourceId
import space.livedigital.sdk.media.video.CameraManager
import space.livedigital.sdk.media.video.CameraManagerDelegate
import space.livedigital.sdk.media.video.CameraPosition
import space.livedigital.sdk.media.video.visual_effects.VideoSourceType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow

class CallViewModel(
    private val callRepository: CallRepository,
    private val contactsRepository: ContactsRepository,
    private val conferenceBackend: ConferenceBackend
) : ViewModel(), KoinComponent {

    val state
        get() = mutableState

    val events
        get() = eventsChannel.receiveAsFlow()

    private val mutableState = MutableStateFlow(ScreenState())

    private val eventsChannel = Channel<ScreenEvent>(Channel.UNLIMITED)

    private var localParticipantId: String? = null
    private var session: ChannelSession? = null
    private var liveDigitalEngine: LiveDigitalEngine? = null
    private var roomAlias: String? = null
    private var timer: Job? = null
    private var startConferenceJob: Job? = null
    private var isLocalVideoPaused = false
    private var isEngineDestroying = false

    init {
        viewModelScope.launch {
            launch {
                callRepository.currentCallState.collect { callState ->
                    val wasActive = state.value.callState is CallState.Active
                    mutableState.update {
                        mutableState.value.copy(callState = callState)
                    }

                    handleCallState(callState, wasActive)
                }
            }

            launch {
                callRepository.audioState.collect { audioState ->
                    mutableState.update {
                        it.copy(
                            audioDevices = audioState.availableEndpoints,
                            currentAudioDevice = audioState.currentEndpoint
                        )
                    }
                }
            }
        }
    }

    fun onCallFinishedBySystem(cause: DisconnectCause) {
        callRepository.dispatchCallAction(
            CallAction.Disconnect(
                displayName = state.value.callState.call.displayName,
                phone = state.value.callState.call.phone,
                roomAlias = state.value.callState.call.roomAlias,
                callType = state.value.callState.call.callType,
                cause = cause
            )
        )
        destroyEngine()
    }

    fun onFlipCameraButtonClicked() {
        liveDigitalEngine?.cameraManager?.flipCamera()
    }

    fun onChangeLocalPreviewFullscreenStateClicked() {
        mutableState.update {
            it.copy(isLocalPreviewFullscreen = !it.isLocalPreviewFullscreen)
        }
    }

    fun onAudioDevicePickerClicked() {
        mutableState.update {
            it.copy(
                isAudioDevicePickerShown = !it.isAudioDevicePickerShown
            )
        }
    }

    fun onAudioDevicePickerDismissed() {
        mutableState.update {
            it.copy(
                isAudioDevicePickerShown = false
            )
        }
    }

    fun onAppBecameFocused() {
        if (isLocalVideoPaused) {
            startLocalVideo()
            isLocalVideoPaused = false
        }
    }

    fun onAppBecameUnfocused() {
        if (mutableState.value.isLocalVideoOn) {
            isLocalVideoPaused = true
            stopLocalVideo()
        }
    }

    fun onAudioDevicePicked(callEndpoint: GeneralCallEndpoint) {
        viewModelScope.launch {
            callRepository.dispatchCallCommand(CallCommand.ChangeEndpoint(callEndpoint))
        }
    }

    private suspend fun handleCallState(
        callState: CallState,
        wasActive: Boolean?
    ) {
        when (callState) {
            is CallState.Active -> {
                if (liveDigitalEngine == null) {
                    liveDigitalEngine = get<LiveDigitalEngine>()
                }

                if (session == null && startConferenceJob == null) {
                    roomAlias = callState.call.roomAlias
                    startConferenceJob = viewModelScope.launch {
                        startConference()
                    }
                }

                if (callState.isMuted) {
                    stopLocalAudio()
                } else {
                    startLocalAudio()
                }

                if (callState.isCameraOn) {
                    startLocalVideo()
                } else {
                    stopLocalVideo()
                }

                if (timer?.isActive != true) {
                    timer = viewModelScope.launch {
                        while (true) {
                            if (session != null) {
                                mutableState.update {
                                    it.copy(callDuration = callState.startTimeMark.elapsedNow())
                                }
                            }
                            delay(1.seconds)
                        }
                    }
                }
            }

            is CallState.Activated -> {
                if (liveDigitalEngine == null) {
                    liveDigitalEngine = get<LiveDigitalEngine>()
                }

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                if (callState.isMuted) {
                    stopLocalAudio()
                } else {
                    startLocalAudio()
                }

                if (callState.isCameraOn) {
                    startLocalVideo()
                } else {
                    stopLocalVideo()
                }
            }

            is CallState.Answered -> {
                if (liveDigitalEngine == null) {
                    liveDigitalEngine = get<LiveDigitalEngine>()
                }

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                if (callState.isMuted) {
                    stopLocalAudio()
                } else {
                    startLocalAudio()
                }

                if (callState.isCameraOn) {
                    startLocalVideo()
                } else {
                    stopLocalVideo()
                }
            }

            is CallState.Outgoing -> {
                if (liveDigitalEngine == null) {
                    liveDigitalEngine = get<LiveDigitalEngine>()
                }

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                if (callState.isMuted) {
                    stopLocalAudio()
                } else {
                    startLocalAudio()
                }

                if (callState.isCameraOn) {
                    startLocalVideo()
                } else {
                    stopLocalVideo()
                }
            }

            is CallState.Ended -> {
                createContactIfMissing(wasActive, callState.call)

                timer?.cancel()
                timer = null

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }

                destroyEngine()
            }

            is CallState.Missed -> {
                timer?.cancel()
                timer = null

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }

                destroyEngine()
            }

            else -> {
                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                timer?.cancel()
                timer = null
            }
        }
    }

    private suspend fun startConference() {
        val roomAlias = roomAlias ?: return
        val joinParams = conferenceBackend.resolveJoinParams(roomAlias) ?: return
        localParticipantId = joinParams.participantId

        if (liveDigitalEngine == null) {
            liveDigitalEngine = get<LiveDigitalEngine>()
        }

        initDelegates()

        connectToChannel(
            signalingToken = joinParams.signalingToken,
            spaceId = joinParams.spaceId,
            roomId = joinParams.roomId
        )
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
    }

    private fun connectToChannel(
        signalingToken: String,
        spaceId: String,
        roomId: String
    ) {
        Log.d(TAG, "connect to channel")
        val appData = PeerAppData(
            name = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        val appDataJson = JsonUtils.encodeToJsonString(appData)

        val channelSessionParams = StockChannelSessionParams(
            signalingToken = signalingToken,
            appData = JSONObject(appDataJson),
            analyticsMetaKeyValues = emptyMap()
        )

        liveDigitalEngine?.connectToChannel(
            channelSessionParams = channelSessionParams,
            delegate = createChannelSessionDelegate(spaceId, roomId),
            successAction = {
                session = it
                startConferenceJob = null
            }
        )
    }

    private fun createChannelSessionDelegate(
        spaceId: String,
        roomId: String
    ): ChannelSessionDelegate {
        return object : ChannelSessionDelegate {
            override fun peersJoined(peers: List<Peer>) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    for (peer in peers) {
                        if (peer.id == session?.myPeerId) {
                            // TODO: On demand, modify sdk to be able to show your remote peer
                            break
                        }

                        Log.d(TAG, "Peer ${peer.id} joined")
                        val peerWithUpdateTime = PeerWithVersion(
                            peer = peer,
                            version = markNow(),
                        )
                        mutableState.update {
                            mutableState.value.copy(remotePeers = it.remotePeers + peerWithUpdateTime)
                        }
                    }
                }
            }

            override fun peerDisconnected(peerId: PeerId) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    mutableState.update {
                        val peers = it.remotePeers.toMutableList()
                        peers.removeIf { it.peer.id == peerId }
                        mutableState.value.copy(remotePeers = peers.toList())
                    }
                }
            }

            override fun peerCanStartVideo(peer: Peer, label: MediaLabel) {
                session?.startVideo(peer, label)
            }

            override fun peerStartedVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    peer.setIsVideoConsumerCanBeImmediatelyResumed(label, true)
                }
            }

            override fun peerCanResumeVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    session?.proceedVideo(peer, label)
                }
            }

            override fun peerResumedVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    val peerWithUpdateTime = PeerWithVersion(peer = peer, version = markNow())
                    mutableState.update {
                        mutableState.value.copy(
                            remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                                it.peer.id == peer.id
                            })
                    }
                }
            }

            override fun peerCanPauseVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    session?.suspendVideo(peer, label)
                }
            }

            override fun peerPausedVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    val peerWithUpdateTime = PeerWithVersion(peer = peer, version = markNow())
                    mutableState.update { it ->
                        it.copy(
                            remotePeers = it.remotePeers.replaceBy(
                                peerWithUpdateTime
                            ) {
                                it.peer.id == peer.id
                            })
                    }
                }
            }

            override fun peerStoppedVideo(peer: Peer, label: MediaLabel) {
                if (state.value.callState.call.callType == CallType.VIDEO) {
                    val peerWithUpdateTime = PeerWithVersion(peer = peer, version = markNow())
                    mutableState.update {
                        mutableState.value.copy(
                            remotePeers = it.remotePeers.replaceBy(peerWithUpdateTime) {
                                it.peer.id == peer.id
                            }
                        )
                    }
                }
            }

            override fun peerCanStartAudio(peer: Peer, label: MediaLabel) {
                session?.startAudio(peer, label)
            }

            override fun peerStartedAudio(peer: Peer, label: MediaLabel) {}

            override fun peerCanResumeAudio(peer: Peer, label: MediaLabel) {
                session?.proceedAudio(peer, label)
            }

            override fun peerResumedAudio(peer: Peer, label: MediaLabel) {}

            override fun peerCanPauseAudio(peer: Peer, label: MediaLabel) {
                session?.suspendAudio(peer, label)
            }

            override fun peerPausedAudio(peer: Peer, label: MediaLabel) {}

            override fun peerStoppedAudio(peer: Peer, label: MediaLabel) {}

            override fun peerAppDataUpdated(peerId: PeerId, appData: JSONObject) {}

            override fun peerPermissionsUpdated(peerId: PeerId, permissions: List<MediaLabel>) {}

            override fun startedLocalAudio(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun stoppedLocalAudio(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun startedLocalVideo(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun stoppedLocalVideo(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun forceStoppedLocalMedia(label: MediaLabel) {}

            override fun connectedToChannel() {
                viewModelScope.launch {
                    joinRoom(spaceId, roomId)
                }
            }

            override fun reconnectedToChannel() {}

            override fun disconnectedFromChannel() {}

            override fun onStatusChanged(status: ChannelSessionStatus) {
                mutableState.update {
                    it.copy(sessionStatus = status)
                }

                if (status == ChannelSessionStatus.STOPPED) restartSession()
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

    private suspend fun joinRoom(spaceId: String, roomId: String) {
        val participantId = localParticipantId
        if (participantId == null) {
            Log.e(TAG, "Failed to join room: missing participantId")
            return
        }

        conferenceBackend.joinRoom(participantId, spaceId, roomId)
    }

    private fun startLocalVideo() {
        if (state.value.isLocalVideoOn) return

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
        if (state.value.isLocalAudioOn) return

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
        destroyEngine(onComplete = { startConference() })
    }

    private fun destroyEngine(onComplete: (suspend () -> Unit)? = null) {
        if (isEngineDestroying) return
        val engine = liveDigitalEngine ?: return
        isEngineDestroying = true

        stopLocalVideo()
        stopLocalAudio()

        engine.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {
                viewModelScope.launch {
                    session = null
                    conferenceBackend.logout()
                    isLocalVideoPaused = false
                    mutableState.update { it.copy(remotePeers = emptyList()) }
                    if (liveDigitalEngine === engine) liveDigitalEngine = null
                    isEngineDestroying = false
                    onComplete?.invoke()
                }
            }
        })
    }

    private suspend fun createContactIfMissing(
        wasActive: Boolean?,
        call: Call,
    ) {
        if (wasActive == true) {
            val phone = call.phone
            val callerName = call.displayName
            createContactIfMissing(phone = phone, callerName = callerName)
        }
    }

    private suspend fun createContactIfMissing(phone: String, callerName: String) {
        val hasContactResult = contactsRepository.hasContact(phone)

        if (hasContactResult == HasContactResult.MISSED) {
            eventsChannel.send(
                ScreenEvent.CreateContact(
                    callerName = callerName,
                    phone = phone
                )
            )
        }
    }

    companion object {
        const val TAG = "LivedigitalAndroidSdkExample"
    }
}

private fun <T> List<T>.replaceBy(item: T, predicate: (T) -> Boolean): List<T> =
    (filterNot(predicate) + item)
