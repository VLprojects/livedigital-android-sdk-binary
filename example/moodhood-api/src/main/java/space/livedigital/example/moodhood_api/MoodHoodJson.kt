package space.livedigital.example.moodhood_api

import kotlinx.serialization.json.Json

/**
 * JSON configuration used across the MoodHood client. Kept module-local so `:moodhood-api`
 * stays self-contained (no dependency back on `:shared`).
 */
internal val MoodHoodJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
