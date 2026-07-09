package space.livedigital.example.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.livedigital.example.calls.push.PushTokenListener

/**
 * Re-registers the device on the process-wide [applicationScope] whenever FCM rotates the token.
 * [DeviceRepository.registerIfSignedIn] re-reads the now-current (new) token.
 */
internal class DeviceTokenListener(
    private val deviceRepository: DeviceRepository,
    private val applicationScope: CoroutineScope
) : PushTokenListener {

    override fun onNewToken(token: String) {
        applicationScope.launch { deviceRepository.registerIfSignedIn() }
    }
}
