package space.livedigital.example.moodhood_api.result.network

sealed class NetworkError {
    class HTTP(val data: HTTPErrorData) : NetworkError()
    class Failure(val throwable: NetworkThrowable) : NetworkError()
}