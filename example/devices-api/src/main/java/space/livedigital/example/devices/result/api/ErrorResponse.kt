package space.livedigital.example.devices.result.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String?,
    val code: String?
)
