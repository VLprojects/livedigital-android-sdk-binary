package space.livedigital.example.entities

import kotlinx.serialization.Serializable

@Serializable
internal data class Room(
    val id: String?,
    val appId: String?,
    val channelId: String?,
)