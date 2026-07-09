package space.livedigital.example.devices.result.api

sealed class ExecutionResult<out Data> {
    class Success<Data>(val data: Data) : ExecutionResult<Data>()
    class Error(val error: ExecutionError) : ExecutionResult<Nothing>()
}
