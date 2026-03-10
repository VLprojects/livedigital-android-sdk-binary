package space.livedigital.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorSystem(
    val primary: Color,
    val accentBase: Color,
    val accent01: Color,
    val accent02: Color,
    val accent03: Color,
    val secondary02: Color,
    val secondary03: Color,
    val contrast: Color,
    val successBase: Color,
    val success01: Color,
    val errorBase: Color,
    val error01: Color,
)

val LocalColorSystem = staticCompositionLocalOf {
    ColorSystem(
        primary = Color.Unspecified,
        accentBase = Color.Unspecified,
        accent01 = Color.Unspecified,
        accent02 = Color.Unspecified,
        accent03 = Color.Unspecified,
        secondary02 = Color.Unspecified,
        secondary03 = Color.Unspecified,
        contrast = Color.Unspecified,
        successBase = Color.Unspecified,
        success01 = Color.Unspecified,
        errorBase = Color.Unspecified,
        error01 = Color.Unspecified
    )
}

val DefaultColorSystem = ColorSystem(
    primary = Color(0x99060A2D),
    accentBase = Color(0xFF1D51FE),
    accent01 = Color(0xFF1A2657),
    accent02 = Color(0xFF6185FE),
    accent03 = Color(0xFFA5B9FF),
    secondary02 = Color(0xFF525566),
    secondary03 = Color(0xFF7F8290),
    contrast = Color(0xFFFFFFFF),
    successBase = Color(0xFF06AA2A),
    success01 = Color(0xFF1F5D2F),
    errorBase = Color(0xFFE01C08),
    error01 = Color(0xFF5F0A01)
)
