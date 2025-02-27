package space.livedigital.example.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonUtils {

    inline fun <reified T> encodeToJsonString(value: T): String {
        return getKotlinSerializationJson().encodeToString(value)
    }

    inline fun <reified T> decodeFromJsonString(jsonString: String): T {
        return getKotlinSerializationJson().decodeFromString<T>(jsonString)
    }

    fun getKotlinSerializationJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}