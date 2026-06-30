package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.AuthStorage
import space.livedigital.example.AuthViewModel
import space.livedigital.example.Permission
import space.livedigital.example.PermissionsViewModel
import space.livedigital.example.backend.PushPayloadConferenceBackend
import space.livedigital.example.calls.CallViewModel
import space.livedigital.example.calls.backend.ConferenceBackend
import space.livedigital.example.calls.repositories.AndroidContactsRepository
import space.livedigital.example.calls.repositories.CallRepository

internal val viewModelsModule = module {
    single<ConferenceBackend> { PushPayloadConferenceBackend() }

    viewModel {
        CallViewModel(
            callRepository = CallRepository.instance ?: CallRepository.create(),
            contactsRepository = AndroidContactsRepository(
                contentResolver = androidContext().contentResolver
            ),
            conferenceBackend = get()
        )
    }

    viewModel {
        AuthViewModel(
            authStorage = AuthStorage.instance ?: AuthStorage.create(androidContext())
        )
    }

    viewModel { (initialPermissions: List<Permission>, isPhoneAccountRegistered: Boolean) ->
        PermissionsViewModel(
            initialPermissions = initialPermissions,
            isPhoneAccountEnabled = isPhoneAccountRegistered
        )
    }
}
