package space.livedigital.example.moodhood_api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import space.livedigital.example.entities.MoodhoodParticipant
import space.livedigital.example.moodhood_api.request.MoodhoodUserTokenRequestBody
import space.livedigital.example.entities.MoodhoodUserToken
import space.livedigital.example.entities.Room
import space.livedigital.example.entities.SignalingToken
import space.livedigital.example.moodhood_api.request.JoinRoomRequestBody
import space.livedigital.example.moodhood_api.request.MoodhoodParticipantRequestBody

/**
 * API methods for working with MoodHoodApi
 */
internal interface MoodHoodApiService {

    @POST("v1/auth/token")
    suspend fun createMoodhoodAPIToken(
        @Body body: MoodhoodUserTokenRequestBody
    ): MoodhoodUserToken

    @GET("v1/spaces/{spaceId}/rooms/{roomId}")
    suspend fun getRoomById(
        @Path("spaceId") spaceId: String?,
        @Path("roomId") roomId: String?,
    ): Room

    @POST("v1/spaces/{SPACE_ID}/participants")
    suspend fun createParticipant(
        @Path("SPACE_ID") spaceId: String,
        @Body body: MoodhoodParticipantRequestBody
    ): MoodhoodParticipant

    @POST("v1/spaces/{spaceId}/participants/{participantId}/signaling-token")
    suspend fun getSignalingToken(
        @Path("spaceId") spaceId: String?,
        @Path("participantId") participantId: String?
    ): SignalingToken

    @POST("v1/spaces/{spaceId}/rooms/{roomId}/join")
    suspend fun joinRoom(
        @Path("spaceId") spaceId: String?,
        @Path("roomId") roomId: String?,
        @Body body: JoinRoomRequestBody
    )
}