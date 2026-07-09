package space.livedigital.example.moodhood_api.request

import kotlinx.serialization.Serializable

@Serializable
data class JoinRoomRequestBody(
    val participantId: String?,
)