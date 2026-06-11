package space.livedigital.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        CompositionLocalProvider(
            LocalColorSystem provides DefaultColorSystem,
            LocalTypographySystem provides createTypographySystem(),
            LocalButtonSystem provides createButtonSystem(DefaultColorSystem),
            content = content
        )
    }
}

internal object AppTheme {
    val colorSystem: ColorSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalColorSystem.current

    val typographySystem: TypographySystem
        @Composable
        @ReadOnlyComposable
        get() = LocalTypographySystem.current

    val buttonSystem: ButtonSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalButtonSystem.current
}