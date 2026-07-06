package space.livedigital.example.device

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import space.livedigital.example.AuthStorage
import space.livedigital.example.BuildConfig
import space.livedigital.example.devices.DevicesApiClient
import space.livedigital.example.devices.result.ApplicationThrowable
import space.livedigital.example.devices.result.api.ExecutionError
import space.livedigital.example.devices.result.api.ExecutionResult
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

internal class DeviceRepository(
    private val authStorage: AuthStorage
) {

    private val client: DevicesApiClient? by lazy {
        val baseUrl = BuildConfig.DEVICES_BASE_URL
        if (baseUrl.isBlank()) {
            Log.w(TAG, "DEVICES_BASE_URL is empty — device registration disabled")
            return@lazy null
        }
        runCatching { DevicesApiClient(baseUrl = baseUrl, apiKey = BuildConfig.DEVICES_API_KEY) }
            .onFailure { Log.e(TAG, "Invalid DEVICES_BASE_URL '$baseUrl': ${it.message}") }
            .getOrNull()
    }

    suspend fun register(phoneNumber: String): ExecutionResult<Unit>? {
        val client = client ?: return null
        val pushToken = currentPushToken()
            ?: return ExecutionResult.Error(
                ExecutionError.Failure(
                    ApplicationThrowable(IllegalStateException("No FCM token"))
                )
            )

        return client.registerDevice(
            deviceId = authStorage.deviceId,
            pushToken = pushToken,
            phoneNumber = phoneNumber,
            locale = currentLocale(),
            deviceName = currentDeviceName(),
            timezone = currentTimeZone()
        ).also { logResult("registerDevice", it) }
    }

    suspend fun unregister(): ExecutionResult<Unit>? {
        val client = client ?: return null
        return client.deleteDevice(deviceId = authStorage.deviceId)
            .also { logResult("deleteDevice", it) }
    }

    suspend fun registerIfSignedIn() {
        val phoneNumber = authStorage.phoneNumber
        if (phoneNumber.isNotBlank()) register(phoneNumber)
    }

    private suspend fun currentPushToken(): String? =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> continuation.resume(token) }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to get FCM token: ${error.message}")
                    continuation.resume(null)
                }
        }

    private fun currentLocale(): String = Locale.getDefault().language

    private fun currentTimeZone(): String = TimeZone.getDefault().id

    private fun currentDeviceName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}"

    private fun logResult(request: String, result: ExecutionResult<Unit>) {
        when (result) {
            is ExecutionResult.Success -> Log.i(TAG, "$request succeeded")
            is ExecutionResult.Error -> when (val error = result.error) {
                is ExecutionError.Expected ->
                    Log.e(TAG, "$request failed: ${error.data.code} ${error.data.message}")

                is ExecutionError.Failure ->
                    Log.e(TAG, "$request failed: ${error.throwable.cause?.message}")
            }
        }
    }

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
