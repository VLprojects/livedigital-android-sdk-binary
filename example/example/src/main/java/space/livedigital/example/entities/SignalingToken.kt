package space.livedigital.example.entities

import kotlinx.serialization.Serializable

@Serializable
internal data class SignalingToken(
    val signalingToken: String?
)