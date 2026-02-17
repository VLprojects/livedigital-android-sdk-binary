package space.livedigital.example.calls.use_cases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import space.livedigital.example.calls.entities.CallState
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

class GetCallStateUseCase(
    private val telecomCallRepository: TelecomCallRepository,
    private val callRepository: CallRepository
) {
    operator fun invoke(): Flow<CallState> {
        return combine(
            telecomCallRepository.currentCallState,
            callRepository.currentCallState
        ) { telecomCallState, callState ->
            when {
                telecomCallState is CallState.Registered -> telecomCallState
                callState is CallState.Registered -> callState
                telecomCallState is CallState.Unregistered -> telecomCallState
                callState is CallState.Unregistered -> callState
                else -> CallState.None
            }
        }.distinctUntilChanged()
    }
}