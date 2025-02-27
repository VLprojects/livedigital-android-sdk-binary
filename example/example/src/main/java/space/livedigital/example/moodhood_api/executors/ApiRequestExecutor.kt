package space.livedigital.example.moodhood_api.executors

import space.livedigital.example.moodhood_api.result.api.parser.ErrorDataParser
import space.livedigital.example.moodhood_api.result.network.NetworkError
import space.livedigital.example.moodhood_api.result.network.NetworkResult
import space.livedigital.example.moodhood_api.result.network.NetworkThrowable
import space.livedigital.example.moodhood_api.result.api.ExecutionError
import space.livedigital.example.moodhood_api.result.api.ExecutionResult

internal class ApiRequestExecutor {

    suspend fun <ResponseBody> execute(
        request: suspend () -> ResponseBody,
    ): ExecutionResult<ResponseBody> {
        val result: NetworkResult<ResponseBody> = NetworkRequestExecutor().execute {
            request.invoke()
        }

        return when (result) {
            is NetworkResult.Success -> ExecutionResult.Success(result.responseBody)
            is NetworkResult.Error -> {
                val executionError = convertNetworkErrorToExecutionError(result.error)
                ExecutionResult.Error(executionError)
            }
        }
    }

    private fun convertNetworkErrorToExecutionError(networkError: NetworkError): ExecutionError {
        return when (networkError) {
            is NetworkError.HTTP -> parseErrorData(networkError.data.errorBody)
            is NetworkError.Failure -> ExecutionError.Failure(networkError.throwable)
        }
    }

    private fun parseErrorData(errorBody: String?): ExecutionError {
        return try {
            val errorData = ErrorDataParser().parse(errorBody)
            ExecutionError.Expected(errorData)
        } catch (throwable: Throwable) {
            val networkThrowable = NetworkThrowable.ServerError(cause = throwable)
            ExecutionError.Failure(networkThrowable)
        }
    }
}