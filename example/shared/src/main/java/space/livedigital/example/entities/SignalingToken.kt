package space.livedigital.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class SignalingToken(
    val signalingToken: String?
)