package space.livedigital.example.devices.result.api.parser

import space.livedigital.example.devices.DevicesJson
import space.livedigital.example.devices.result.api.ErrorData
import space.livedigital.example.devices.result.api.ErrorResponse

class ErrorDataParser {

    private val json = DevicesJson

    fun parse(errorBodyText: String?): ErrorData {
        if (errorBodyText == null) return ErrorData(message = null, code = null)

        val errorResponse = json.decodeFromString<ErrorResponse>(errorBodyText)

        return ErrorData(errorResponse.message, errorResponse.code)
    }
}
