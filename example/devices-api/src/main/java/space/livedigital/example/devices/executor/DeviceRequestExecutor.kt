package space.livedigital.example.devices.executor

import space.livedigital.example.devices.result.DeviceRequestResult
import kotlin.coroutines.cancellation.CancellationException

class DeviceRequestExecutor {

    suspend fun <ResponseBody> execute(
        request: suspend () -> ResponseBody,
    ): DeviceRequestResult<ResponseBody> {
        return try {
            DeviceRequestResult.Success(request())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            DeviceRequestResult.Error(throwable)
        }
    }
}
