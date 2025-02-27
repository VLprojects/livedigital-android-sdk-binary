package space.livedigital.example.moodhood_api.result.api.parser

import space.livedigital.example.moodhood_api.result.api.ErrorData
import space.livedigital.example.moodhood_api.result.api.ErrorResponse
import space.livedigital.example.utils.JsonUtils

internal class ErrorDataParser {

    private val json = JsonUtils.getKotlinSerializationJson()

    fun parse(errorBodyText: String?): ErrorData {
        if (errorBodyText == null) return ErrorData(message = null, code = null)

        val errorResponse = json.decodeFromString<ErrorResponse>(errorBodyText)

        return ErrorData(errorResponse.message, errorResponse.code)
    }
}