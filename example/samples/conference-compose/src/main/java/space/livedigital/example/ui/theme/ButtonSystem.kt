package space.livedigital.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class ButtonSystem(
    val acceptButtonStyle: ButtonStyle,
    val rejectButtonStyle: ButtonStyle,
    val primaryButtonStyle: ButtonStyle,
    val tertiaryButtonStyle: ButtonStyle,
) {

    data class ButtonStyle(
        val normalContainerColor: Color,
        val pressedContainerColor: Color,
        val normalContentColor: Color,
        val pressedContentColor: Color
    ) {
        companion object {
            val unspecified = ButtonStyle(
                normalContainerColor = Color.Unspecified,
                pressedContainerColor = Color.Unspecified,
                normalContentColor = Color.Unspecified,
                pressedContentColor = Color.Unspecified
            )
        }
    }
}

internal val LocalButtonSystem = staticCompositionLocalOf {
    ButtonSystem(
        acceptButtonStyle = ButtonSystem.ButtonStyle.unspecified,
        rejectButtonStyle = ButtonSystem.ButtonStyle.unspecified,
        primaryButtonStyle = ButtonSystem.ButtonStyle.unspecified,
        tertiaryButtonStyle = ButtonSystem.ButtonStyle.unspecified,
    )
}

@Composable
@ReadOnlyComposable
internal fun createButtonSystem(colorSystem: ColorSystem): ButtonSystem {
    return ButtonSystem(
        acceptButtonStyle = createAcceptButtonStyle(colorSystem),
        rejectButtonStyle = createRejectButtonStyle(colorSystem),
        primaryButtonStyle = createPrimaryButtonStyle(colorSystem),
        tertiaryButtonStyle = createTertiaryButtonStyle(colorSystem),
    )
}

private fun createAcceptButtonStyle(colorSystem: ColorSystem): ButtonSystem.ButtonStyle {
    return ButtonSystem.ButtonStyle(
        normalContainerColor = colorSystem.successBase,
        normalContentColor = colorSystem.contrast,
        pressedContainerColor = colorSystem.success01,
        pressedContentColor = colorSystem.contrast
    )
}

private fun createRejectButtonStyle(colorSystem: ColorSystem): ButtonSystem.ButtonStyle {
    return ButtonSystem.ButtonStyle(
        normalContainerColor = colorSystem.errorBase,
        normalContentColor = colorSystem.contrast,
        pressedContainerColor = colorSystem.error01,
        pressedContentColor = colorSystem.contrast
    )
}

private fun createPrimaryButtonStyle(colorSystem: ColorSystem): ButtonSystem.ButtonStyle {
    return ButtonSystem.ButtonStyle(
        normalContainerColor = colorSystem.contrast,
        normalContentColor = colorSystem.accentBase,
        pressedContainerColor = colorSystem.accent03,
        pressedContentColor = colorSystem.accentBase
    )
}

private fun createTertiaryButtonStyle(colorSystem: ColorSystem): ButtonSystem.ButtonStyle {
    return ButtonSystem.ButtonStyle(
        normalContainerColor = colorSystem.contrast.copy(alpha = 0.2f),
        normalContentColor = colorSystem.errorBase,
        pressedContainerColor = colorSystem.accent03,
        pressedContentColor = colorSystem.errorBase
    )
}
