package space.livedigital.example.moodhood_api.request

import kotlinx.serialization.Serializable

@Serializable
data class MoodhoodParticipantRequestBody(
    val name: String,
    val role: String,
    val roomId: String,
    val clientUniqueId: String
)