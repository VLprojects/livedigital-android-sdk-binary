package space.livedigital.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionsViewModel(
    initialPermissions: List<Permission>,
    isPhoneAccountEnabled: Boolean
) : ViewModel() {

    val permissionsState
        get() = _permissionsState.asStateFlow()
    private val _permissionsState = MutableStateFlow(
        PermissionsState(
            permissions = initialPermissions,
            phoneAccountEnabledState = PhoneAccountEnabledState(isPhoneAccountEnabled)
        )
    )

    fun onPermissionStateUpdated(permissionName: String, isGranted: Boolean) {
        _permissionsState.update { state ->
            val updatedPermissions = state.permissions.map { permission ->
                if (permission.name == permissionName) permission.copy(isGranted = isGranted)
                else permission
            }
            state.copy(permissions = updatedPermissions)
        }
    }

    fun onPhoneAccountEnabledStateUpdated(isEnabled: Boolean) {
        _permissionsState.update {
            it.copy(
                phoneAccountEnabledState = it.phoneAccountEnabledState.copy(
                    isEnabled = isEnabled
                )
            )
        }
    }
}

data class PermissionsState(
    val permissions: List<Permission>,
    val phoneAccountEnabledState: PhoneAccountEnabledState
)

data class Permission(
    val name: String,
    val importance: Importance,
    val isGranted: Boolean
)

data class PhoneAccountEnabledState(
    val isEnabled: Boolean,
    val importance: Importance = Importance.IMPORTANT_FOR_SYSTEM_CALLS
)

enum class Importance {
    DEFAULT,
    IMPORTANT_FOR_ALL_CALLS,
    IMPORTANT_FOR_SYSTEM_CALLS
}


