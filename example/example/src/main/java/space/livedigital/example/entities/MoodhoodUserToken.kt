package space.livedigital.example.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MoodhoodUserToken(
    @SerialName("token_type")
    val tokenType: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("access_token")
    val accessToken: String?
)