package space.livedigital.example.ui.components.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.livedigital.example.ui.theme.AppTheme

@Composable
internal fun ContainerComponent(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    contentPadding: PaddingValues = ContainerComponentDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides AppTheme.colorSystem.contrast,
        LocalTextStyle provides AppTheme.typographySystem.mainTextMedium
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = modifier
                .wrapContentSize()
                .background(
                    color = AppTheme.colorSystem.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(contentPadding),
            content = content
        )
    }
}
