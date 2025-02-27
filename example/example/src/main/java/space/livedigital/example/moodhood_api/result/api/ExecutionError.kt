package space.livedigital.example.moodhood_api.result.api

import space.livedigital.example.moodhood_api.result.ApplicationThrowable

internal sealed class ExecutionError {
    class Expected(val data: ErrorData) : ExecutionError()
    class Failure(val throwable: ApplicationThrowable) : ExecutionError()
}