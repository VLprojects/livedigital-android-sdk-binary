package space.livedigital.example.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import space.livedigital.example.AuthState
import space.livedigital.example.Importance
import space.livedigital.example.Permission
import space.livedigital.example.PermissionsState
import space.livedigital.example.R
import space.livedigital.example.calls.utils.XiaomiUtilities
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun MainScreen(
    permissionsState: State<PermissionsState>,
    authState: State<AuthState>,
    onCopyButtonClicked: () -> Unit,
    onPermissionSwitchClicked: (permission: Permission) -> Unit,
    onCallAccountSwitchClicked: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val isPushPermissionGranted = permissionsState.value.permissions
        .find { it.name == Manifest.permission.POST_NOTIFICATIONS }
        ?.isGranted ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .gradientBackground(
                listOf(
                    AppTheme.colorSystem.accent02,
                    AppTheme.colorSystem.accent03
                )
            )
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CopyButtonContainerComponent(onCopyButtonClicked = onCopyButtonClicked)

        AuthContainerComponent(
            authState = authState,
            isPushPermissionGranted = isPushPermissionGranted,
            onPhoneNumberChanged = onPhoneNumberChanged,
            onLoginClicked = onLoginClicked,
            onLogoutClicked = onLogoutClicked
        )

        PermissionsContainerComponent(
            permissionsState = permissionsState,
            onPermissionSwitchClicked = onPermissionSwitchClicked,
            onCallAccountSwitchClicked = onCallAccountSwitchClicked
        )
    }
}

@Composable
private fun AuthContainerComponent(
    authState: State<AuthState>,
    isPushPermissionGranted: Boolean,
    onPhoneNumberChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    val authState = authState.value

    ContainerComponent(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.description_auth))

        if (!isPushPermissionGranted) {
            Text(
                text = stringResource(R.string.label_auth_permission_required),
                color = AppTheme.colorSystem.accent03
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = authState.phoneNumber,
                onValueChange = onPhoneNumberChanged,
                enabled = isPushPermissionGranted && !authState.isLoggedIn,
                singleLine = true,
                label = { Text(text = stringResource(R.string.label_phone_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = authTextFieldColors(),
                modifier = Modifier
                    .weight(1.0f)
                    .align(Alignment.CenterVertically)
            )

            if (authState.isLoggedIn) {
                ButtonComponent(
                    onClick = onLogoutClicked,
                    enabled = isPushPermissionGranted,
                    style = AppTheme.buttonSystem.rejectButtonStyle,
                    text = stringResource(R.string.button_logout),
                )
            } else {
                ButtonComponent(
                    onClick = onLoginClicked,
                    enabled = isPushPermissionGranted && authState.phoneNumber.isNotBlank(),
                    style = AppTheme.buttonSystem.primaryButtonStyle,
                    text = stringResource(R.string.button_login),
                )
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppTheme.colorSystem.contrast,
    unfocusedTextColor = AppTheme.colorSystem.contrast,
    disabledTextColor = AppTheme.colorSystem.contrast.copy(alpha = 0.5f),
    cursorColor = AppTheme.colorSystem.contrast,
    focusedBorderColor = AppTheme.colorSystem.accentBase,
    unfocusedBorderColor = AppTheme.colorSystem.secondary03,
    disabledBorderColor = AppTheme.colorSystem.secondary03.copy(alpha = 0.5f),
    focusedLabelColor = AppTheme.colorSystem.accent03,
    unfocusedLabelColor = AppTheme.colorSystem.secondary03,
    disabledLabelColor = AppTheme.colorSystem.secondary03.copy(alpha = 0.5f)
)

@Composable
private fun CopyButtonContainerComponent(onCopyButtonClicked: () -> Unit) {
    ContainerComponent(
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.description_copy_token))
        ButtonComponent(
            onClick = onCopyButtonClicked,
            style = AppTheme.buttonSystem.primaryButtonStyle,
            text = stringResource(R.string.button_copy_token_to_clipboards)
        )
    }
}

@Composable
private fun PermissionsContainerComponent(
    permissionsState: State<PermissionsState>,
    onPermissionSwitchClicked: (permission: Permission) -> Unit,
    onCallAccountSwitchClicked: () -> Unit
) {
    val permissionsState = permissionsState.value

    ContainerComponent(
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.label_importance_description))

        permissionsState.permissions.forEach { permission ->
            SwitchWithImportance(
                label = permissionLabel(permission.name),
                isChecked = permission.isGranted,
                importance = permission.importance,
                onSwitchClicked = {
                    onPermissionSwitchClicked(permission)
                }
            )
        }

        Text(text = stringResource(R.string.description_call_account))

        SwitchWithImportance(
            label = stringResource(R.string.label_phone_account_enabled),
            isChecked = permissionsState.phoneAccountEnabledState.isEnabled,
            importance = permissionsState.phoneAccountEnabledState.importance,
            onSwitchClicked = onCallAccountSwitchClicked
        )
    }
}

@Composable
private fun SwitchWithImportance(
    label: String,
    importance: Importance,
    isChecked: Boolean,
    onSwitchClicked: () -> Unit
) {
    val circleColor = when (importance) {
        Importance.IMPORTANT_FOR_SYSTEM_CALLS -> AppTheme.colorSystem.accentBase
        Importance.IMPORTANT_FOR_ALL_CALLS -> AppTheme.colorSystem.errorBase
        Importance.DEFAULT -> AppTheme.colorSystem.secondary03
    }

    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1.0f)
        ) {
            Text(text = label, modifier = Modifier.weight(1.0f, fill = false))

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .minimumInteractiveComponentSize()
                    .background(color = circleColor, shape = CircleShape)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { onSwitchClicked() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colorSystem.accentBase,
                uncheckedThumbColor = AppTheme.colorSystem.secondary03,
                checkedTrackColor = AppTheme.colorSystem.accent03,
                uncheckedTrackColor = AppTheme.colorSystem.secondary03.copy(alpha = 0.2f),
            )
        )
    }
}

@Composable
private fun permissionLabel(permission: String): String {
    return when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> {
            stringResource(R.string.label_notifications)
        }

        Manifest.permission.READ_PHONE_STATE -> {
            stringResource(R.string.label_phone_state)
        }

        Manifest.permission.CALL_PHONE -> {
            stringResource(R.string.label_call_phone)
        }

        Manifest.permission.READ_CONTACTS -> {
            stringResource(R.string.label_read_contacts)
        }

        Manifest.permission.WRITE_CONTACTS -> {
            stringResource(R.string.label_write_contacts)
        }

        Manifest.permission.RECORD_AUDIO -> {
            stringResource(R.string.label_microphone)
        }

        Manifest.permission.BLUETOOTH_CONNECT -> {
            stringResource(R.string.label_bluetooth)
        }

        Manifest.permission.CAMERA -> {
            stringResource(R.string.label_camera)
        }

        XiaomiUtilities.OP_SHOW_WHEN_LOCKED.toString() -> {
            stringResource(R.string.label_xiaomi_lock_screen)
        }

        XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY.toString() -> {
            stringResource(R.string.label_xiaomi_open_from_background)
        }

        else -> permission
    }
}

@Composable
private fun XiaomiPermissionsContainerComponent(
    onXiaomiDialerSettingsButtonClicked: () -> Unit
) {
    val context = LocalContext.current

    var canShowOnLockScreen by remember {
        mutableStateOf(
            XiaomiUtilities.isCustomPermissionGranted(
                context,
                XiaomiUtilities.OP_SHOW_WHEN_LOCKED
            )
        )
    }
    var canStartFromBackground by remember {
        mutableStateOf(
            XiaomiUtilities.isCustomPermissionGranted(
                context,
                XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY
            )
        )
    }

    LifecycleResumeEffect(Unit) {
        canShowOnLockScreen = XiaomiUtilities.isCustomPermissionGranted(
            context,
            XiaomiUtilities.OP_SHOW_WHEN_LOCKED
        )
        canStartFromBackground = XiaomiUtilities.isCustomPermissionGranted(
            context,
            XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY
        )

        onPauseOrDispose {

        }
    }

    Text(
        text = stringResource(R.string.label_xiaomi_permissions_info),
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SwitchWithImportance(
            label = stringResource(R.string.label_xiaomi_open_from_background),
            importance = Importance.IMPORTANT_FOR_SYSTEM_CALLS,
            isChecked = canStartFromBackground,
            onSwitchClicked = onXiaomiDialerSettingsButtonClicked
        )
        SwitchWithImportance(
            label = stringResource(R.string.label_xiaomi_lock_screen),
            importance = Importance.DEFAULT,
            isChecked = canShowOnLockScreen,
            onSwitchClicked = onXiaomiDialerSettingsButtonClicked
        )
    }
}