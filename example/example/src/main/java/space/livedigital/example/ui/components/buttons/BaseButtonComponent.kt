package space.livedigital.example.ui.components.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun BaseButtonComponent(
    onClick: () -> Unit,
    style: ButtonSystem.ButtonStyle,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = ButtonDefaults.ContentPadding,
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
        contentPadding = paddingValues,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        interactionSource = interactionSource,
        content = content,
        modifier = modifier
    )
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 480
)
@Composable
private fun ButtonComponentPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colorSystem.primary),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BaseButtonComponent(onClick = {}, style = AppTheme.buttonSystem.acceptButtonStyle) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_round_call_24),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }

            BaseButtonComponent(onClick = {}, style = AppTheme.buttonSystem.rejectButtonStyle) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_round_call_24),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }

            BaseButtonComponent(onClick = {}, style = AppTheme.buttonSystem.primaryButtonStyle) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_round_call_24),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }

            BaseButtonComponent(onClick = {}, style = AppTheme.buttonSystem.tertiaryButtonStyle) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_round_call_24),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}