package space.livedigital.example.ui.extensions

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal fun Modifier.gradientBackground(
    colors: List<Color>,
): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = colors,
        start = Offset.Zero,
        end = Offset.Infinite
    )
)