package space.livedigital.example.moodhood_api.result.network

internal sealed class NetworkResult<out ResponseBody> {
    class Success<ResponseBody>(val responseBody: ResponseBody) : NetworkResult<ResponseBody>()
    class Error(val error: NetworkError) : NetworkResult<Nothing>()
}