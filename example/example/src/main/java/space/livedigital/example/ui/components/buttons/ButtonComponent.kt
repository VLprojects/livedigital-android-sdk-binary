package space.livedigital.example.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.livedigital.example.R
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.example.ui.theme.ButtonSystem

@Composable
internal fun ButtonComponent(
    onClick: () -> Unit,
    style: ButtonSystem.ButtonStyle,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    contentDescription: String? = null
) {
    BaseButtonComponent(
        onClick = onClick,
        style = style,
        contentPadding = ButtonComponentDefaults.ContentPadding,
        modifier = modifier
    ) {
        icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }

        if (icon != null && text != null) {
            Spacer(modifier = Modifier.size(8.dp))
        }

        text?.let { text ->
            Text(text = text, style = AppTheme.typographySystem.mainTextMedium)
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 420,
    heightDp = 620
)
@Composable
private fun ButtonComponentPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(color = AppTheme.colorSystem.primary)
        ) {
            ButtonComponent(
                onClick = {},
                icon = ImageVector.vectorResource(R.drawable.ic_checkbox),
                style = AppTheme.buttonSystem.acceptButtonStyle
            )

            ButtonComponent(
                onClick = {},
                text = "Перезвонить",
                style = AppTheme.buttonSystem.primaryButtonStyle
            )

            ButtonComponent(
                onClick = {},
                icon = ImageVector.vectorResource(R.drawable.ic_refresh),
                text = "Повторить звонок",
                style = AppTheme.buttonSystem.primaryButtonStyle
            )
        }
    }
}