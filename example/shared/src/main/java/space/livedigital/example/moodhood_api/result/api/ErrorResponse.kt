package space.livedigital.example.moodhood_api.result.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String?,
    val code: String?
)