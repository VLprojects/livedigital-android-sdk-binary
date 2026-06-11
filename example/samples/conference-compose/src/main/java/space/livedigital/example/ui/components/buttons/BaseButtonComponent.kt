package space.livedigital.example.ui.components.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.livedigital.example.ui.theme.ButtonSystem

@Composable
internal fun BaseButtonComponent(
    onClick: () -> Unit,
    style: ButtonSystem.ButtonStyle,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonComponentDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            style.pressedContainerColor
        } else {
            style.normalContainerColor
        },
        label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isPressed) {
            style.pressedContentColor
        } else {
            style.normalContentColor
        },
        label = "contentColor"
    )

    Button(
        onClick = onClick,
        contentPadding = contentPadding,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        interactionSource = interactionSource,
        content = content,
        modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
    )
}
