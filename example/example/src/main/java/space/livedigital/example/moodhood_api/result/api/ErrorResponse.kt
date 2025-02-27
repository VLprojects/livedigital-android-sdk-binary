package space.livedigital.example.moodhood_api.result.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ErrorResponse(
    val message: String?,
    val code: String?
)