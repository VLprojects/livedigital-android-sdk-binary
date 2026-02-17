package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.MainViewModel
import space.livedigital.example.calls.use_cases.GetCallStateUseCase
import space.livedigital.example.calls.CallViewModel
import space.livedigital.example.calls.use_cases.EndCallUseCase
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

val viewModelsModule = module {
    viewModel {
        val telecomCallRepository = TelecomCallRepository.instance
            ?: TelecomCallRepository.create()
        val callRepository = CallRepository.instance ?: CallRepository.create(androidContext())

        CallViewModel(
            getCallStateUseCase = GetCallStateUseCase(
                telecomCallRepository = telecomCallRepository,
                callRepository = callRepository
            ),
            endCallUseCase = EndCallUseCase(
                telecomCallRepository = telecomCallRepository,
                callRepository = callRepository
            )
        )
    }
    viewModel {
        MainViewModel(
            callRepository = CallRepository.instance ?: CallRepository.create(androidContext()),
            contentResolver = androidContext().contentResolver
        )
    }
}
