package space.livedigital.example.backend

import space.livedigital.example.calls.backend.ConferenceBackend
import space.livedigital.example.calls.backend.JoinParams
import space.livedigital.example.devices.DevicesApiClient

class PushPayloadConferenceBackend(
    @Suppress("unused") // wired now; used once the push-based flow + device registration land
    private val devicesApiClient: DevicesApiClient
) : ConferenceBackend {

    override suspend fun resolveJoinParams(roomAlias: String): JoinParams? {
        // TODO(LK-6568): build JoinParams from the push payload instead of returning null.
        return null
    }

    override suspend fun joinRoom(participantId: String, spaceId: String, roomId: String) {
        // TODO(LK-6568): no-op until the push-based join flow is implemented.
    }

    override suspend fun logout() {
        // TODO(LK-6568): deregister the device via DevicesApiClient.deleteDevice.
    }
}
