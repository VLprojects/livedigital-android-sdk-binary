package space.livedigital.example.devices.result.network

sealed class NetworkError {
    class HTTP(val data: HTTPErrorData) : NetworkError()
    class Failure(val throwable: NetworkThrowable) : NetworkError()
}
