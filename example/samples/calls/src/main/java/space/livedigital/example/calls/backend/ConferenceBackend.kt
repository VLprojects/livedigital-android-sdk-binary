package space.livedigital.example.calls.backend

interface ConferenceBackend {

    suspend fun resolveJoinParams(roomAlias: String): JoinParams?

    suspend fun joinRoom(participantId: String, spaceId: String, roomId: String)

    suspend fun logout()
}

data class JoinParams(
    val channelId: String,
    val participantId: String,
    val signalingToken: String,
    val spaceId: String,
    val roomId: String,
)
