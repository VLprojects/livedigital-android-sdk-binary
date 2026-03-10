package space.livedigital.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val colorSystem = DefaultColorSystem
    val typographySystem = createTypographySystem(colorSystem)
    val buttonSystem = createButtonSystem(colorSystem)
    MaterialTheme {
        CompositionLocalProvider(
            LocalColorSystem provides colorSystem,
            LocalTypographySystem provides typographySystem,
            LocalButtonSystem provides buttonSystem,
            content = content
        )
    }
}

object AppTheme {
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