package space.livedigital.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorSystem = if (isDarkTheme) LightColorSystem else DarkColorSystem
    val typographySystem = createTypographySystem(colorSystem)
    MaterialTheme {
        CompositionLocalProvider(
            LocalColorSystem provides colorSystem,
            LocalTypographySystem provides typographySystem,
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
}