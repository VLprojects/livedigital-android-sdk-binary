package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.Permission
import space.livedigital.example.PermissionsViewModel
import space.livedigital.example.calls.CallViewModel
import space.livedigital.example.calls.repositories.AndroidContactsRepository
import space.livedigital.example.calls.repositories.CallRepository

internal val viewModelsModule = module {
    viewModel {
        CallViewModel(
            callRepository = CallRepository.instance ?: CallRepository.create(),
            contactsRepository = AndroidContactsRepository(
                contentResolver = androidContext().contentResolver
            )
        )
    }

    viewModel { (initialPermissions: List<Permission>, isPhoneAccountRegistered: Boolean) ->
        PermissionsViewModel(
            initialPermissions = initialPermissions,
            isPhoneAccountEnabled = isPhoneAccountRegistered
        )
    }
}
