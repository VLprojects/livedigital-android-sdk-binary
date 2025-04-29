package space.livedigital.example.moodhood_api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import space.livedigital.example.entities.MoodhoodParticipant
import space.livedigital.example.entities.MoodhoodUserToken
import space.livedigital.example.entities.Room
import space.livedigital.example.entities.SignalingToken
import space.livedigital.example.moodhood_api.executors.ApiRequestExecutor
import space.livedigital.example.moodhood_api.request.JoinRoomRequestBody
import space.livedigital.example.moodhood_api.request.MoodhoodParticipantRequestBody
import space.livedigital.example.moodhood_api.request.MoodhoodUserTokenRequestBody
import space.livedigital.example.moodhood_api.result.api.ExecutionResult
import space.livedigital.example.utils.JsonUtils
import space.livedigital.sdk.BuildConfig
import java.util.concurrent.TimeUnit

internal class MoodHoodApiClient(
    private val baseUrl: String
) {
    private val apiRequestExecutor = ApiRequestExecutor()

    suspend fun authorizeAsGuest(
        clientId: String,
        clientSecret: String,
        grantType: String
    ): ExecutionResult<MoodhoodUserToken> {
        val body = MoodhoodUserTokenRequestBody(clientId, clientSecret, grantType)
        val result = apiRequestExecutor.execute { apiService.createMoodhoodAPIToken(body) }

        if (result is ExecutionResult.Success) configureRequestHeaders(clientId, result.data)

        return result
    }

    suspend fun logout(): ExecutionResult<Unit> {
        configureRequestHeaders(clientId = null, userToken = null)
        return ExecutionResult.Success(Unit)
    }

    suspend fun getRoom(spaceId: String, roomId: String): ExecutionResult<Room> {
        return apiRequestExecutor.execute { apiService.getRoomById(spaceId, roomId) }
    }

    suspend fun createParticipant(
        name: String,
        role: String,
        clientUniqueId: String,
        spaceId: String,
        roomId: String
    ): ExecutionResult<MoodhoodParticipant> {
        val body = MoodhoodParticipantRequestBody(name, role, roomId, clientUniqueId)
        return apiRequestExecutor.execute { apiService.createParticipant(spaceId, body) }
    }

    suspend fun getSignalingToken(
        spaceId: String,
        participantId: String
    ): ExecutionResult<SignalingToken> {
        return apiRequestExecutor.execute { apiService.getSignalingToken(spaceId, participantId) }
    }

    suspend fun joinRoom(
        participantId: String,
        spaceId: String,
        roomId: String
    ): ExecutionResult<Unit> {
        val body = JoinRoomRequestBody(participantId)
        return apiRequestExecutor.execute { apiService.joinRoom(spaceId, roomId, body) }
    }

    private val loggingInterceptor = createLoggingInterceptor()
    private val headersInterceptor = createHeaderInterceptor()
    private val apiService = createRetrofit().create(MoodHoodApiService::class.java)

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(createConverterFactory())
            .client(createOkHttpClient())
            .build()
    }

    private fun createConverterFactory(): Converter.Factory {
        val contentType = "application/json".toMediaType()
        return JsonUtils.getKotlinSerializationJson().asConverterFactory(contentType)
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                connectTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                readTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                writeTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                addInterceptor(headersInterceptor)
                addNetworkInterceptor(loggingInterceptor)
            }.build()
    }

    private fun createLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
    }

    private fun createHeaderInterceptor(): Interceptor {
        return HeadersInterceptor()
    }

    private fun configureRequestHeaders(clientId: String?, userToken: MoodhoodUserToken?) {
        (headersInterceptor as HeadersInterceptor).apply {
            setClientId(clientId)
            setAccessToken(userToken?.tokenType, userToken?.accessToken)
        }
    }

    private companion object {
        const val API_TIMEOUT_IN_MILLISECONDS: Long = 60_000
    }
}