package space.livedigital.example.entities

import kotlinx.serialization.Serializable

/**
 * Unified room model shared by all samples.
 *
 * Each sample historically used a slightly different subset of these fields:
 *  - conference (XML)     -> id, appId, channelId
 *  - conference (Compose) -> + name
 *  - calls                -> + spaceId
 *
 * All non-essential fields are nullable so every sample can rely on the same type.
 */
@Serializable
data class Room(
    val id: String?,
    val appId: String?,
    val channelId: String?,
    val name: String? = null,
    val spaceId: String? = null,
)
