package space.livedigital.example.devices

import kotlinx.serialization.json.Json

/**
 * JSON configuration used across the devices client. Kept module-local so `:devices-api`
 * stays self-contained (no dependency back on `:shared`).
 */
internal val DevicesJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
