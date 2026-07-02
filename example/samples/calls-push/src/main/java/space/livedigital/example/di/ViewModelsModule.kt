package space.livedigital.example.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.AuthStorage
import space.livedigital.example.AuthViewModel
import space.livedigital.example.Permission
import space.livedigital.example.PermissionsViewModel
import space.livedigital.example.calls.CallViewModel
import space.livedigital.example.calls.push.PushTokenListener
import space.livedigital.example.calls.repositories.AndroidContactsRepository
import space.livedigital.example.calls.repositories.CallRepository
import space.livedigital.example.device.DeviceRepository
import space.livedigital.example.device.DeviceTokenListener

internal val viewModelsModule = module {
    // Process-wide scope for fire-and-forget device work (app start, FCM token refresh).
    // Intentionally never cancelled — it lives for the whole process.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    single {
        DeviceRepository(
            authStorage = AuthStorage.instance ?: AuthStorage.create(
                androidContext()
            )
        )
    }

    single<PushTokenListener> {
        DeviceTokenListener(deviceRepository = get(), applicationScope = get())
    }

    viewModel {
        CallViewModel(
            callRepository = CallRepository.instance ?: CallRepository.create(),
            contactsRepository = AndroidContactsRepository(
                contentResolver = androidContext().contentResolver
            )
        )
    }

    viewModel {
        AuthViewModel(
            authStorage = AuthStorage.instance ?: AuthStorage.create(androidContext()),
            deviceRepository = get()
        )
    }

    viewModel { (initialPermissions: List<Permission>, isPhoneAccountRegistered: Boolean) ->
        PermissionsViewModel(
            initialPermissions = initialPermissions,
            isPhoneAccountEnabled = isPhoneAccountRegistered
        )
    }
}
