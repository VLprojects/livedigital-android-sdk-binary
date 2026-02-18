package space.livedigital.example.calls

import android.os.Build
import android.telecom.DisconnectCause
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
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.repositories.HasContactResult
import space.livedigital.example.calls.use_cases.EndCallUseCase
import space.livedigital.example.calls.use_cases.GetCallStateUseCase
import space.livedigital.example.calls.use_cases.HasContactUseCase
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
import space.livedigital.sdk.media.audio.AudioSource
import kotlin.time.TimeMark
import kotlin.time.TimeSource.Monotonic.markNow

class CallViewModel(
    private val getCallStateUseCase: GetCallStateUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val hasContactUseCase: HasContactUseCase
) : ViewModel(), KoinComponent {


    val state
        get() = mutableState

    val events
        get() = eventsChannel.receiveAsFlow()

    private val mutableState = MutableStateFlow(ScreenState())

    private val eventsChannel = Channel<ScreenEvent>(Channel.UNLIMITED)
    private val apiClient = MoodHoodApiClient(MOODHOOD_API_URL)

    private var localParticipantId: String? = null
    private var session: ChannelSession? = null
    private var liveDigitalEngine: LiveDigitalEngine? = null
    private var roomAlias: String? = null

    init {
        viewModelScope.launch {
            getCallStateUseCase.invoke().collect { callState ->
                val wasMuted = (state.value.callState as? CallState.Registered)?.isMuted
                val wasActive = (state.value.callState as? CallState.Registered)?.isActive
                mutableState.update {
                    mutableState.value.copy(callState = callState)
                }

                handleCallState(callState, wasMuted, wasActive)
            }
        }
    }

    fun onCallFinishedBySystem(cause: DisconnectCause) {
        endCallUseCase.invoke(cause)
        liveDigitalEngine?.destroy(object : LiveDigitalEngineDestroyDelegate {
            override fun onDestroyed() {
                viewModelScope.launch {
                    stopLocalAudio()
                    session = null
                    apiClient.logout()
                }
            }
        })
    }

    private suspend fun handleCallState(
        callState: CallState,
        wasMuted: Boolean?,
        wasActive: Boolean?
    ) {
        when (callState) {
            CallState.None -> {}

            is CallState.Registered -> {
                val isIncomingCall = session == null && callState.isActive

                if (isIncomingCall) {
                    roomAlias = callState.roomAlias
                    startConference()
                }

                if (wasMuted == null) return

                val isMuted = callState.isMuted

                if (wasMuted != isMuted) {
                    if (mutableState.value.isLocalAudioOn) {
                        stopLocalAudio()
                    } else {
                        startLocalAudio()
                    }
                }
            }

            is CallState.Unregistered -> {
                createContactIfMissing(wasActive, callState)

                liveDigitalEngine?.destroy(object : LiveDigitalEngineDestroyDelegate {
                    override fun onDestroyed() {
                        viewModelScope.launch {
                            stopLocalAudio()
                            session = null
                            apiClient.logout()
                        }
                    }
                })
            }
        }
    }

    private suspend fun startConference() {
        authorize()

        val room = getRoom() ?: return
        val roomId = room.id ?: return
        val spaceId = room.spaceId ?: return
        val channelId = room.channelId
        if (channelId == null) {
            Log.e(TAG, "Error getting channelId from: $room")
            return
        }

        val participant = createParticipant(spaceId, roomId) ?: return
        val participantId = participant.id
        localParticipantId = participantId
        if (participantId == null) {
            Log.e(TAG, "Error getting participantId from: $participant")
            return
        }

        val signalingTokenResult = getSignalingToken(participantId, spaceId)
        val signalingToken = signalingTokenResult?.signalingToken
        if (signalingToken == null) {
            Log.e(TAG, "Error getting signalingToken from: $signalingTokenResult")
            return
        }

        liveDigitalEngine = get<LiveDigitalEngine>()

        initDelegates()

        connectToChannel(channelId, participantId, signalingToken, spaceId, roomId)
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
        val roomAlias = roomAlias ?: return null
        val roomResult = apiClient.getRoomByAlias(roomAlias)
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

    private suspend fun createParticipant(spaceId: String, roomId: String): MoodhoodParticipant? {
        val participantResult = apiClient.createParticipant(
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            role = "host",
            clientUniqueId = BSONObjectIdGenerator.generateBSONObjectId(),
            spaceId = spaceId,
            roomId = roomId
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

    private suspend fun getSignalingToken(participantId: String, spaceId: String): SignalingToken? {
        val signalingTokenResult = apiClient.getSignalingToken(spaceId, participantId)
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
        liveDigitalEngine?.delegate = object : LiveDigitalEngineDelegate {
            override fun engineFailed(error: LiveDigitalEngineError) {
                Log.e(TAG, "Engine failed $error")
            }
        }
    }

    private fun connectToChannel(
        channelId: String,
        participantId: String,
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
            delegate = createChannelSessionDelegate(spaceId, roomId),
            successAction = {
                session = it
            }
        )
    }

    private fun createChannelSessionDelegate(
        spaceId: String,
        roomId: String
    ): ChannelSessionDelegate {
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
                    joinRoom(spaceId, roomId)
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

    private suspend fun joinRoom(spaceId: String, roomId: String) {
        val participantId = localParticipantId
        if (participantId == null) {
            Log.e(TAG, "Failed to join room: missing participantId")
            return
        }

        val joinRoomResult = apiClient.joinRoom(participantId, spaceId, roomId)
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
                    stopLocalAudio()
                    session = null
                    apiClient.logout()
                    startConference()
                }
            }
        })
    }

    private suspend fun createContactIfMissing(
        wasActive: Boolean?,
        callState: CallState.Unregistered
    ) {
        if (wasActive == true) {
            val phone = callState.callAttributes.address.schemeSpecificPart
            val callerName = callState.callAttributes.displayName.toString()
            createContactIfMissing(phone = phone, callerName = callerName)
        }
    }

    private suspend fun createContactIfMissing(phone: String, callerName: String) {
        val hasContactResult =
            hasContactUseCase.invoke(phone)

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
        const val MOODHOOD_API_URL = "https://moodhood-api.livedigital.space/"
        const val MOODHOOD_CLIENT_ID = "moodhood-demo"
        const val MOODHOOD_CLIENT_SECRET = "demo12345abcde6789zxcvDemo"
        const val CLIENT_CREDENTIALS_GARANT_TYPE = "client_credentials"

        const val TAG = "LivedigitalAndroidSdkExample"
    }
}

private fun <T> List<T>.replaceBy(item: T, predicate: (T) -> Boolean): List<T> =
    (filterNot(predicate) + item)

data class ScreenState(
    val remotePeers: List<PeerWithUpdateTime> = emptyList(),
    val isLocalAudioOn: Boolean = false,
    val localAudioSource: AudioSource? = null,
    val callState: CallState = CallState.None,
)

sealed interface ScreenEvent {

    data class CreateContact(val callerName: String, val phone: String) : ScreenEvent
    data object CloseCall : ScreenEvent
}

data class PeerWithUpdateTime(val peer: Peer, val updateTimeMark: TimeMark)