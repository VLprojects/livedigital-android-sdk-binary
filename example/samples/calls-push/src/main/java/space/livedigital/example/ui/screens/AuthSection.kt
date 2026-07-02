package space.livedigital.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import space.livedigital.example.AuthState
import space.livedigital.example.R
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun AuthContainerComponent(
    authState: State<AuthState>,
    isPushPermissionGranted: Boolean,
    onPhoneNumberChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit
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
                onValueChange = { onPhoneNumberChanged(it) },
                enabled = isPushPermissionGranted &&
                        !authState.isAuthorizedIn &&
                        !authState.isLoading,
                singleLine = true,
                label = { Text(text = stringResource(R.string.label_phone_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = authTextFieldColors(),
                modifier = Modifier
                    .weight(1.0f)
                    .align(Alignment.CenterVertically)
            )

            when {
                authState.isLoading -> CircularProgressIndicator(
                    color = AppTheme.colorSystem.contrast,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(24.dp)
                )

                authState.isAuthorizedIn -> ButtonComponent(
                    onClick = onSignOutClicked,
                    enabled = isPushPermissionGranted,
                    style = AppTheme.buttonSystem.rejectButtonStyle,
                    text = stringResource(R.string.button_logout),
                )

                else -> ButtonComponent(
                    onClick = onSignInClicked,
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
