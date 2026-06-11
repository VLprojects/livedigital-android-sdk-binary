package space.livedigital.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import space.livedigital.example.R

@Immutable
internal data class TypographySystem(
    val mainTextMedium: TextStyle,
)

internal val LocalTypographySystem = staticCompositionLocalOf {
    TypographySystem(
        mainTextMedium = TextStyle.Default
    )
}

@Composable
@ReadOnlyComposable
internal fun createTypographySystem(): TypographySystem {
    val manropeFontFamily = FontFamily(
        Font(R.font.manrope_bold, FontWeight.Bold),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
    )
    val appMainMedium = TextStyle(
        fontFamily = manropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    return TypographySystem(mainTextMedium = appMainMedium)
}
