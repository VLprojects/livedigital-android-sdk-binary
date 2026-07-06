package space.livedigital.example.devices.result.network

sealed class NetworkResult<out ResponseBody> {
    class Success<ResponseBody>(val responseBody: ResponseBody) : NetworkResult<ResponseBody>()
    class Error(val error: NetworkError) : NetworkResult<Nothing>()
}
