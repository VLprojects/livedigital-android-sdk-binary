package space.livedigital.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.livedigital.example.device.DeviceRepository
import space.livedigital.example.devices.result.DeviceRequestResult

internal class AuthViewModel(
    private val authStorage: AuthStorage,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val authState
        get() = mutableAuthState.asStateFlow()
    private val mutableAuthState = MutableStateFlow(
        AuthState(
            phoneNumber = authStorage.phoneNumber,
            isAuthorizedIn = authStorage.phoneNumber.isNotBlank(),
            isLoading = false
        )
    )

    fun onPhoneNumberChanged(phoneNumber: String) {
        val state = mutableAuthState.value
        if (state.isAuthorizedIn || state.isLoading) {
            return
        }

        mutableAuthState.update { it.copy(phoneNumber = phoneNumber) }
    }

    fun onSignInClicked() {
        val state = mutableAuthState.value

        if (state.isAuthorizedIn || state.isLoading || state.phoneNumber.isBlank()) {
            return
        }

        mutableAuthState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = deviceRepository.register(state.phoneNumber)
            val signedIn = result == null || result is DeviceRequestResult.Success
            if (signedIn) {
                authStorage.phoneNumber = state.phoneNumber
                mutableAuthState.update { it.copy(isAuthorizedIn = true, isLoading = false) }
            } else {
                mutableAuthState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSignOutClicked() {
        signOut()
    }

    fun onPushPermissionStateChanged(isGranted: Boolean) {
        if (!isGranted) {
            signOut()
        }
    }

    private fun signOut() {
        val state = mutableAuthState.value
        if (!state.isAuthorizedIn || state.isLoading) {
            return
        }

        mutableAuthState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            deviceRepository.unregister()
            authStorage.phoneNumber = ""
            mutableAuthState.update {
                it.copy(isAuthorizedIn = false, isLoading = false, phoneNumber = "")
            }
        }
    }
}

internal data class AuthState(
    val phoneNumber: String,
    val isAuthorizedIn: Boolean,
    val isLoading: Boolean = false
)
