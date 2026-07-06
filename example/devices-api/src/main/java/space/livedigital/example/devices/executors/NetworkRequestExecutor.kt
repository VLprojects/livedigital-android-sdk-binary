package space.livedigital.example.devices.executors

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import space.livedigital.example.devices.result.network.HTTPErrorData
import space.livedigital.example.devices.result.network.NetworkError
import space.livedigital.example.devices.result.network.NetworkResult
import space.livedigital.example.devices.result.network.NetworkThrowable
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException

class NetworkRequestExecutor {

    suspend fun <ResponseBody> execute(
        request: suspend () -> ResponseBody,
    ): NetworkResult<ResponseBody> {
        return try {
            val responseBody: ResponseBody = request.invoke()
            NetworkResult.Success(responseBody)
        } catch (throwable: Throwable) {
            val networkError: NetworkError = convertThrowableToNetworkError(throwable)
            NetworkResult.Error(networkError)
        }
    }

    private fun convertThrowableToNetworkError(throwable: Throwable): NetworkError {
        return when (throwable) {
            is HttpException -> {
                val errorBody = getErrorBodyFromHttpException(throwable)
                val errorData = HTTPErrorData(throwable.code(), errorBody)
                NetworkError.HTTP(errorData)
            }

            else -> {
                val networkThrowable = convertThrowableToNetworkThrowable(throwable)
                NetworkError.Failure(networkThrowable)
            }
        }
    }

    private fun getErrorBodyFromHttpException(throwable: HttpException): String? {
        return throwable.response()?.errorBody()?.string()
    }

    private fun convertThrowableToNetworkThrowable(throwable: Throwable): NetworkThrowable {
        return when (throwable) {
            is CancellationException -> throw throwable
            is IOException -> NetworkThrowable.IO(cause = throwable)
            is TimeoutException -> NetworkThrowable.Timeout(cause = throwable)
            is SerializationException -> NetworkThrowable.Parsing(cause = throwable)
            else -> NetworkThrowable.Unknown(cause = throwable)
        }
    }
}
