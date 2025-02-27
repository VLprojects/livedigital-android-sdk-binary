package space.livedigital.example.moodhood_api.result.network

import space.livedigital.example.moodhood_api.result.ApplicationThrowable

internal sealed class NetworkThrowable(cause: Throwable) : ApplicationThrowable(cause) {
    class IO(cause: Throwable) : NetworkThrowable(cause)
    class Timeout(cause: Throwable) : NetworkThrowable(cause)
    class Parsing(cause: Throwable) : NetworkThrowable(cause)
    class Unknown(cause: Throwable) : NetworkThrowable(cause)
    class ServerError(cause: Throwable) : NetworkThrowable(cause)
}