package space.livedigital.example.backend

import space.livedigital.example.calls.backend.ConferenceBackend
import space.livedigital.example.calls.backend.JoinParams

/**
 * Skeleton [ConferenceBackend] for the push-token-service sample. Unlike the MoodHood backend it
 * does not resolve a room over REST — the join data (signalingToken, channelId, participantId, …)
 * is expected to arrive inside the incoming push payload.
 *
 * Left as a stub for now: the push contract is not yet extended and device registration via
 * :devices-api is not wired. See LK-6568.
 */
class PushPayloadConferenceBackend : ConferenceBackend {

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
