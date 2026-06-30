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
            isAuthorizedIn = authStorage.phoneNumber.isNotBlank()
        )
    )

    fun onPhoneNumberChanged(phoneNumber: String) {
        if (mutableAuthState.value.isAuthorizedIn) {
            return
        }

        mutableAuthState.update { it.copy(phoneNumber = phoneNumber) }
    }

    fun onSignInClicked() {
        val phoneNumber = mutableAuthState.value.phoneNumber

        if (mutableAuthState.value.isAuthorizedIn || phoneNumber.isBlank()) {
            return
        }

        authStorage.phoneNumber = phoneNumber
        mutableAuthState.update {
            it.copy(isAuthorizedIn = true)
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
        if (!mutableAuthState.value.isAuthorizedIn) {
            return
        }

        authStorage.phoneNumber = ""
        mutableAuthState.update {
            it.copy(isAuthorizedIn = false, phoneNumber = "")
        }
    }
}

internal data class AuthState(
    val phoneNumber: String,
    val isAuthorizedIn: Boolean
)
