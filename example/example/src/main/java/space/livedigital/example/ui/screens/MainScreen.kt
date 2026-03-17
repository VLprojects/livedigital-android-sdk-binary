package space.livedigital.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.livedigital.example.R
import space.livedigital.example.ui.components.buttons.ButtonComponent
import space.livedigital.example.ui.components.containers.ContainerComponent
import space.livedigital.example.ui.extensions.gradientBackground
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun MainScreen(
    onCopyButtonClicked: () -> Unit,
    onOpenCallAccountSettingsButtonClicked: () -> Unit
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CopyButtonContainerComponent(onCopyButtonClicked)

        OpenCallAccountContainerComponent(onOpenCallAccountSettingsButtonClicked)
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
private fun OpenCallAccountContainerComponent(onOpenCallAccountSettingsButtonClicked: () -> Unit) {
    ContainerComponent(
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.description_call_accounts))

        ButtonComponent(
            onClick = onOpenCallAccountSettingsButtonClicked,
            style = AppTheme.buttonSystem.primaryButtonStyle,
            text = stringResource(R.string.button_open_call_accounts_settings)
        )
    }
}