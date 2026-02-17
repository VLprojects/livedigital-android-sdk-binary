package space.livedigital.example.calls.use_cases

import android.telecom.DisconnectCause
import space.livedigital.example.calls.entities.CallAction
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

class EndCallUseCase(
    private val telecomCallRepository: TelecomCallRepository,
    private val callRepository: CallRepository
) {

    operator fun invoke(cause: DisconnectCause) {
        val action = CallAction.Disconnect(cause)

        (telecomCallRepository.currentCallState.value as? CallState.Registered)?.processAction(
            action
        )
        (callRepository.currentCallState.value as? CallState.Registered)?.processAction(action)
    }
}