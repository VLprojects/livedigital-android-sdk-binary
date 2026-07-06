package space.livedigital.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import space.livedigital.example.R
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun OutgoingCallContainerComponent(
    onCallButtonClicked: (calleePhoneNumber: String) -> Unit
) {
    var calleePhoneNumber by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ContainerComponent(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.description_outgoing_call))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = calleePhoneNumber,
                onValueChange = { calleePhoneNumber = it },
                singleLine = true,
                label = { Text(text = stringResource(R.string.label_callee_phone_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = outgoingCallTextFieldColors(),
                modifier = Modifier
                    .weight(1.0f)
                    .align(Alignment.CenterVertically)
            )

            ButtonComponent(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onCallButtonClicked(calleePhoneNumber)
                    calleePhoneNumber = ""
                },
                enabled = calleePhoneNumber.isNotBlank(),
                style = AppTheme.buttonSystem.primaryButtonStyle,
                text = stringResource(R.string.button_call),
            )
        }
    }
}

@Composable
private fun outgoingCallTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
