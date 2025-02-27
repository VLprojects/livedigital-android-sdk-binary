package space.livedigital.example.moodhood_api.result.network

internal sealed class NetworkError {
    class HTTP(val data: HTTPErrorData) : NetworkError()
    class Failure(val throwable: NetworkThrowable) : NetworkError()
}