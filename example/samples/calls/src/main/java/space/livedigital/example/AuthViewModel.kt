package space.livedigital.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AuthViewModel(private val authStorage: AuthStorage) : ViewModel() {

    val authState
        get() = mutableAuthState.asStateFlow()
    private val mutableAuthState = MutableStateFlow(
        AuthState(
            phoneNumber = authStorage.phoneNumber,
            isLoggedIn = authStorage.phoneNumber.isNotBlank()
        )
    )

    fun onPhoneNumberChanged(phoneNumber: String) {
        if (mutableAuthState.value.isLoggedIn) {
            return
        }

        mutableAuthState.update { it.copy(phoneNumber = phoneNumber) }
    }

    fun onLoginClicked() {
        val phoneNumber = mutableAuthState.value.phoneNumber

        if (mutableAuthState.value.isLoggedIn || phoneNumber.isBlank()) {
            return
        }

        authStorage.phoneNumber = phoneNumber
        mutableAuthState.update {
            it.copy(isLoggedIn = true)
        }
    }

    fun onLogoutClicked() {
        logOut()
    }

    fun onPushPermissionStateChanged(isGranted: Boolean) {
        if (!isGranted) {
            logOut()
        }
    }

    private fun logOut() {
        if (!mutableAuthState.value.isLoggedIn) {
            return
        }

        authStorage.phoneNumber = ""
        mutableAuthState.update {
            it.copy(isLoggedIn = false, phoneNumber = "")
        }
    }
}

internal data class AuthState(
    val phoneNumber: String,
    val isLoggedIn: Boolean
)
