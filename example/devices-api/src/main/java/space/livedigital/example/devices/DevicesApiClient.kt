package space.livedigital.example.devices

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import space.livedigital.example.devices.executors.ApiRequestExecutor
import space.livedigital.example.devices.request.RegisterDeviceRequestBody
import space.livedigital.example.devices.result.api.ExecutionResult
import java.util.concurrent.TimeUnit

class DevicesApiClient(
    apiKey: String,
    private val baseUrl: String,
    private val additionalInterceptors: List<Interceptor> = emptyList(),
) {
    private val apiRequestExecutor = ApiRequestExecutor()

    suspend fun registerDevice(
        deviceId: String,
        pushToken: String,
        phoneNumber: String,
        locale: String,
        deviceName: String,
        timezone: String,
        pushPlatform: String = DEFAULT_PUSH_PLATFORM,
    ): ExecutionResult<Unit> {
        val body = RegisterDeviceRequestBody(
            pushToken = pushToken,
            locale = locale,
            phoneNumber = phoneNumber,
            deviceName = deviceName,
            pushPlatform = pushPlatform,
            timezone = timezone,
        )
        return apiRequestExecutor.execute { apiService.registerDevice(deviceId, body) }
    }

    suspend fun deleteDevice(deviceId: String): ExecutionResult<Unit> {
        return apiRequestExecutor.execute { apiService.deleteDevice(deviceId) }
    }

    private val loggingInterceptor = createLoggingInterceptor()
    private val apiKeyInterceptor = ApiKeyInterceptor(apiKey)
    private val apiService = createRetrofit().create(DevicesApiService::class.java)

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(createConverterFactory())
            .client(createOkHttpClient())
            .build()
    }

    private fun createConverterFactory(): Converter.Factory {
        val contentType = "application/json".toMediaType()
        return DevicesJson.asConverterFactory(contentType)
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                connectTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                readTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                writeTimeout(API_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                addInterceptor(apiKeyInterceptor)
                addNetworkInterceptor(loggingInterceptor)
                additionalInterceptors.forEach { addInterceptor(it) }
            }.build()
    }

    private fun createLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
    }

    private companion object {
        const val API_TIMEOUT_IN_MILLISECONDS: Long = 60_000
        const val DEFAULT_PUSH_PLATFORM = "fcm"
    }
}
