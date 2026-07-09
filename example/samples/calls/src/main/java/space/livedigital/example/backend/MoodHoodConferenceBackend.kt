package space.livedigital.example.backend

import android.os.Build
import android.util.Log
import space.livedigital.example.bson.BSONObjectIdGenerator
import space.livedigital.example.calls.backend.ConferenceBackend
import space.livedigital.example.calls.backend.JoinParams
import space.livedigital.example.entities.MoodhoodParticipant
import space.livedigital.example.entities.Room
import space.livedigital.example.entities.SignalingToken
import space.livedigital.example.moodhood_api.MoodHoodApiClient
import space.livedigital.example.moodhood_api.result.api.ExecutionError
import space.livedigital.example.moodhood_api.result.api.ExecutionResult

/**
 * [ConferenceBackend] backed by the MoodHood REST API:
 * guest auth → room (by alias) → participant → signaling token → join.
 */
class MoodHoodConferenceBackend : ConferenceBackend {

    private val apiClient = MoodHoodApiClient(
        baseUrl = MOODHOOD_API_URL
    )

    override suspend fun resolveJoinParams(roomAlias: String): JoinParams? {
        authorize()

        val room = getRoom(roomAlias) ?: return null
        val roomId = room.id
        val spaceId = room.spaceId
        val channelId = room.channelId
        if (roomId == null || spaceId == null || channelId == null) {
            Log.e(TAG, "Missing room ids in: $room")
            return null
        }

        val participantId = createParticipant(spaceId, roomId)?.id
        if (participantId == null) {
            Log.e(TAG, "Missing participantId")
            return null
        }

        val signalingToken = getSignalingToken(participantId, spaceId)?.signalingToken
        if (signalingToken == null) {
            Log.e(TAG, "Missing signalingToken")
            return null
        }

        return JoinParams(
            channelId = channelId,
            participantId = participantId,
            signalingToken = signalingToken,
            spaceId = spaceId,
            roomId = roomId
        )
    }

    override suspend fun joinRoom(participantId: String, spaceId: String, roomId: String) {
        when (val result = apiClient.joinRoom(participantId, spaceId, roomId)) {
            is ExecutionResult.Success -> Log.i(TAG, "Joined room")
            is ExecutionResult.Error ->
                Log.e(TAG, "Failed to join room: ${result.error.messageOrNull()}")
        }
    }

    override suspend fun logout() {
        apiClient.logout()
    }

    private suspend fun authorize() {
        val result = apiClient.authorizeAsGuest(
            MOODHOOD_CLIENT_ID,
            MOODHOOD_CLIENT_SECRET,
            CLIENT_CREDENTIALS_GRANT_TYPE
        )
        when (result) {
            is ExecutionResult.Success -> Log.i(TAG, "Created user token")
            is ExecutionResult.Error ->
                Log.e(TAG, "Error creating user token: ${result.error.messageOrNull()}")
        }
    }

    private suspend fun getRoom(roomAlias: String): Room? {
        return when (val result = apiClient.getRoomByAlias(roomAlias)) {
            is ExecutionResult.Success -> result.data
            is ExecutionResult.Error -> {
                Log.e(TAG, "Error getting room details: ${result.error.messageOrNull()}")
                null
            }
        }
    }

    private suspend fun createParticipant(spaceId: String, roomId: String): MoodhoodParticipant? {
        val result = apiClient.createParticipant(
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            role = "host",
            clientUniqueId = BSONObjectIdGenerator.generateBSONObjectId(),
            spaceId = spaceId,
            roomId = roomId
        )
        return when (result) {
            is ExecutionResult.Success -> result.data
            is ExecutionResult.Error -> {
                Log.e(TAG, "Error creating participant: ${result.error.messageOrNull()}")
                null
            }
        }
    }

    private suspend fun getSignalingToken(participantId: String, spaceId: String): SignalingToken? {
        return when (val result = apiClient.getSignalingToken(spaceId, participantId)) {
            is ExecutionResult.Success -> result.data
            is ExecutionResult.Error -> {
                Log.e(TAG, "Error creating signaling token: ${result.error.messageOrNull()}")
                null
            }
        }
    }

    private fun ExecutionError.messageOrNull(): String? = when (this) {
        is ExecutionError.Expected -> data.message
        is ExecutionError.Failure -> throwable.message
    }

    private companion object {
        const val TAG = "MoodHoodConfBackend"
        const val MOODHOOD_API_URL = "https://moodhood-api.livedigital.space/"
        const val MOODHOOD_CLIENT_ID = "moodhood-demo"
        const val MOODHOOD_CLIENT_SECRET = "demo12345abcde6789zxcvDemo"
        const val CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials"
    }
}
