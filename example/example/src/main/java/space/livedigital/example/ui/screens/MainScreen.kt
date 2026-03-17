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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.livedigital.example.Importance
import space.livedigital.example.Permission
import space.livedigital.example.PermissionsState
import space.livedigital.example.R
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun MainScreen(
    permissionsState: State<PermissionsState>,
    onCopyButtonClicked: () -> Unit,
    onPermissionSwitchClicked: (permission: Permission) -> Unit,
    onCallAccountSwitchClicked: () -> Unit
) {
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
        CopyButtonContainerComponent(onCopyButtonClicked)

        PermissionsContainerComponent(
            permissionsState,
            onPermissionSwitchClicked,
            onCallAccountSwitchClicked
        )
    }
}

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
    onPermissionsSwitchClicked: (permission: Permission) -> Unit,
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
                    onPermissionsSwitchClicked(permission)
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
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label)

            Box(
                modifier = Modifier
                    .size(12.dp)
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
        Manifest.permission.POST_NOTIFICATIONS -> stringResource(R.string.label_notifications)
        Manifest.permission.READ_PHONE_STATE -> stringResource(R.string.label_phone_state)
        Manifest.permission.CALL_PHONE -> stringResource(R.string.label_call_phone)
        Manifest.permission.READ_CONTACTS -> stringResource(R.string.label_read_contacts)
        Manifest.permission.WRITE_CONTACTS -> stringResource(R.string.label_write_contacts)
        Manifest.permission.RECORD_AUDIO -> stringResource(R.string.label_microphone)
        Manifest.permission.BLUETOOTH_CONNECT -> stringResource(R.string.label_bluetooth)
        else -> permission
    }
}