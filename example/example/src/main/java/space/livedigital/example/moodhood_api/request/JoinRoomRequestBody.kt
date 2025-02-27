package space.livedigital.example.moodhood_api.request

import kotlinx.serialization.Serializable

@Serializable
internal data class JoinRoomRequestBody(
    val participantId: String?,
)