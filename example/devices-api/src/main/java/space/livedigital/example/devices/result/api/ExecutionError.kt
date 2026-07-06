package space.livedigital.example.devices.result.api

import space.livedigital.example.devices.result.ApplicationThrowable

sealed class ExecutionError {
    class Expected(val data: ErrorData) : ExecutionError()
    class Failure(val throwable: ApplicationThrowable) : ExecutionError()
}
