package space.livedigital.example.devices.request

import kotlinx.serialization.Serializable

/**
 * Body of `PUT v1/devices/{deviceId}`. JSON keys already match these names, so no
 * `@SerialName` mapping is needed.
 */
@Serializable
data class RegisterDeviceRequestBody(
    val pushToken: String,
    val locale: String,
    val phoneNumber: String,
    val deviceName: String,
    val pushPlatform: String,
    val timezone: String,
)
