package space.livedigital.example.devices.result

sealed class DeviceRequestResult<out Data> {
    data class Success<Data>(val data: Data) : DeviceRequestResult<Data>()
    data class Error(val throwable: Throwable) : DeviceRequestResult<Nothing>()
}
