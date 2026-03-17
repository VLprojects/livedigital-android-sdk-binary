package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.calls.CallViewModel
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.repositories.AndroidContactsRepository

val viewModelsModule = module {
    viewModel {
        CallViewModel(
            callRepository = CallRepository.instance ?: CallRepository.create(),
            contactsRepository = AndroidContactsRepository(
                contentResolver = androidContext().contentResolver
            )
        )
    }
}
