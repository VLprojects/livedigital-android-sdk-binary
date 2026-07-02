package space.livedigital.example.device

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import space.livedigital.example.AuthStorage
import space.livedigital.example.BuildConfig
import space.livedigital.example.devices.DevicesApiClient
import space.livedigital.example.devices.result.DeviceRequestResult
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

    suspend fun register(phoneNumber: String): DeviceRequestResult<Unit>? {
        val client = client ?: return null
        val pushToken = currentPushToken()
            ?: return DeviceRequestResult.Error(IllegalStateException("No FCM token"))

        return client.registerDevice(
            deviceId = authStorage.deviceId,
            pushToken = pushToken,
            phoneNumber = phoneNumber,
            locale = currentLocale(),
            deviceName = currentDeviceName(),
            timezone = currentTimeZone()
        ).also { logResult("registerDevice", it) }
    }

    suspend fun unregister(): DeviceRequestResult<Unit>? {
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

    private fun logResult(request: String, result: DeviceRequestResult<Unit>) {
        when (result) {
            is DeviceRequestResult.Success -> Log.i(TAG, "$request succeeded")
            is DeviceRequestResult.Error ->
                Log.e(TAG, "$request failed: ${result.throwable.message}")
        }
    }

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
