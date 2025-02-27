package space.livedigital.example.moodhood_api.result.api

internal sealed class ExecutionResult<out Data> {
    class Success<Data>(val data: Data) : ExecutionResult<Data>()
    class Error(val error: ExecutionError) : ExecutionResult<Nothing>()
}