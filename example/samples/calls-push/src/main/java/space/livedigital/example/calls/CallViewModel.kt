package space.livedigital.example.calls

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
import org.koin.core.parameter.parametersOf
import space.livedigital.example.BuildConfig
import space.livedigital.example.calls.entities.Call
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallCommand
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.entities.GeneralCallEndpoint
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.calls.repositories.ContactsRepository
import space.livedigital.example.calls.repositories.HasContactResult
import space.livedigital.example.calls.utils.OutboundCallTokenGenerator
import space.livedigital.sdk.Failure
import space.livedigital.sdk.Success
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CallViewModel(
    private val callRepository: CallRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel(), KoinComponent {

    val state
        get() = mutableState

    val events
        get() = eventsChannel.receiveAsFlow()

    private val mutableState = MutableStateFlow(ScreenState())

    private val eventsChannel = Channel<ScreenEvent>(Channel.UNLIMITED)

    private var session: ChannelSession? = null
    private var liveDigitalEngine: LiveDigitalEngine? = null
    private var signalingToken: String? = null
    private var timer: Job? = null
    private var startConferenceJob: Job? = null
    private var isEngineDestroying = false
    private var isOutboundCallInitiated = false

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
            CallAction.Disconnect(call = state.value.callState.call, cause = cause)
        )
        destroyEngine()
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
                ensureEngine()

                if (session == null && startConferenceJob == null) {
                    signalingToken = callState.call.signalingToken
                    startConferenceJob = viewModelScope.launch {
                        startConference()
                    }
                }

                syncLocalMedia(callState)

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

            is CallState.Outgoing -> {
                ensureEngine()

                if (session == null && startConferenceJob == null) {
                    signalingToken = callState.call.signalingToken
                    startConferenceJob = viewModelScope.launch {
                        startConference()
                    }
                }

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                syncLocalMedia(callState)
            }

            is CallState.WithMedia -> {
                ensureEngine()

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }
                syncLocalMedia(callState)
            }

            is CallState.Ended -> {
                createContactIfMissing(wasActive, callState.call)

                timer?.cancel()
                timer = null
                isOutboundCallInitiated = false

                mutableState.update {
                    it.copy(callDuration = Duration.ZERO)
                }

                destroyEngine()
            }

            is CallState.Missed -> {
                timer?.cancel()
                timer = null
                isOutboundCallInitiated = false

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
                isOutboundCallInitiated = false
            }
        }
    }

    private fun getLiveDigitalEngine(): LiveDigitalEngine = get<LiveDigitalEngine> {
        parametersOf(
            BuildConfig.LOAD_BALANCER_BASE_URL,
            BuildConfig.SIGNALING_API_BASE_URL
        )
    }

    private fun ensureEngine(): LiveDigitalEngine =
        liveDigitalEngine ?: getLiveDigitalEngine().also { liveDigitalEngine = it }

    private fun syncLocalMedia(callState: CallState.WithMedia) {
        if (callState.isMuted) {
            stopLocalAudio()
        } else {
            startLocalAudio()
        }
    }

    private fun startConference() {
        val signalingToken = signalingToken ?: return

        ensureEngine()

        initDelegates()

        connectToChannel(signalingToken)
    }

    /**
     * For an outbound external call, joining the channel by itself doesn't ring the callee:
     * once the local peer is connected, the backend must be asked to originate the PSTN leg.
     * Must happen exactly once per call — session restarts (see [restartSession]) reconnect to
     * the channel but must not dial the callee again, so the flag is only reset when the call
     * reaches a terminal state in [handleCallState].
     */
    private fun initiateOutboundCallIfNeeded() {
        if (isOutboundCallInitiated) return
        val signalingToken = signalingToken ?: return
        if (!OutboundCallTokenGenerator.isOutboundCallToken(signalingToken)) return
        val engine = liveDigitalEngine ?: return

        isOutboundCallInitiated = true
        viewModelScope.launch {
            when (val result = engine.initiateCall()) {
                is Success -> {
                    Log.d(TAG, "Outbound call initiated: ${result.value}")
                }

                is Failure -> {
                    Log.e(TAG, "Failed to initiate outbound call: ${result.error}")
                    callRepository.dispatchCallAction(
                        CallAction.Disconnect(
                            call = state.value.callState.call,
                            cause = DisconnectCause(DisconnectCause.ERROR)
                        )
                    )
                }
            }
        }
    }

    private fun initDelegates() {
        liveDigitalEngine?.delegate = object : LiveDigitalEngineDelegate {
            override fun engineFailed(error: LiveDigitalEngineError) {
                Log.e(TAG, "Engine failed $error")
            }
        }
    }

    private fun activateOutgoingCallIfNeeded(peers: List<Peer>) {
        val callState = callRepository.currentCallState.value

        if (callState !is CallState.Outgoing) {
            return
        }
        if (peers.none { it.id != session?.myPeerId }) {
            return
        }

        callRepository.dispatchCallAction(CallAction.Activate(call = callState.call))
    }

    private fun connectToChannel(signalingToken: String) {
        Log.d(TAG, "connect to channel")

        liveDigitalEngine?.connectToChannel(
            StockChannelSessionParams(
                signalingToken = signalingToken,
                appData = null,
                analyticsMetaKeyValues = emptyMap()
            ),
            createChannelSessionDelegate(),
        ) {
            session = it
            startConferenceJob = null
        }
    }

    private fun createChannelSessionDelegate(): ChannelSessionDelegate {
        return object : ChannelSessionDelegate {
            override fun peersJoined(peers: List<Peer>) {
                activateOutgoingCallIfNeeded(peers)
            }

            override fun peerDisconnected(peerId: PeerId) {
                callRepository.dispatchCallAction(
                    CallAction.Disconnect(
                        call = state.value.callState.call,
                        cause = DisconnectCause(DisconnectCause.REMOTE)
                    )
                )
            }

            // External (PSTN) calls are audio-only, so the video lifecycle callbacks are no-ops.
            override fun peerCanStartVideo(peer: Peer, label: MediaLabel) {}

            override fun peerStartedVideo(peer: Peer, label: MediaLabel) {}

            override fun peerCanResumeVideo(peer: Peer, label: MediaLabel) {}

            override fun peerResumedVideo(peer: Peer, label: MediaLabel) {}

            override fun peerCanPauseVideo(peer: Peer, label: MediaLabel) {}

            override fun peerPausedVideo(peer: Peer, label: MediaLabel) {}

            override fun peerStoppedVideo(peer: Peer, label: MediaLabel) {}

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

            override fun stoppedLocalVideo(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun stoppedLocalAudio(label: MediaLabel, mediaSourceId: MediaSourceId) {}

            override fun forceStoppedLocalMedia(label: MediaLabel) {}

            override fun connectedToChannel() {
                initiateOutboundCallIfNeeded()
            }

            override fun reconnectedToChannel() {}

            override fun disconnectedFromChannel() {}

            override fun onStatusChanged(status: ChannelSessionStatus) {
                mutableState.update {
                    it.copy(sessionStatus = status)
                }

                if (status == ChannelSessionStatus.STOPPED) {
                    restartSession()
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

        stopLocalAudio()

        engine.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {
                viewModelScope.launch {
                    session = null
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
