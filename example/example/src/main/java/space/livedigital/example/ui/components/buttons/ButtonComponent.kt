package space.livedigital.example.ui.components.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import space.livedigital.example.ui.theme.AppTheme
import space.livedigital.example.ui.theme.ButtonSystem

@Composable
internal fun ButtonComponent(
    onClick: () -> Unit,
    style: ButtonSystem.ButtonStyle,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    contentDescription: String? = null,
    contentPaddingValues: PaddingValues = ButtonComponentDefaults.ContentPadding
) {
    BaseButtonComponent(
        onClick = onClick,
        style = style,
        contentPadding = contentPaddingValues,
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
