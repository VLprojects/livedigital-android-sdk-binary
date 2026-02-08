package space.livedigital.example.di

import androidx.core.telecom.CallsManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.SessionViewModel
import space.livedigital.example.calls.utils.CallRepository

val viewModelsModule = module {
    viewModel {
        SessionViewModel(
            savedStateHandle = get(),
            callRepository = CallRepository.instance ?: CallRepository.create(androidContext())
        )
    }
}
