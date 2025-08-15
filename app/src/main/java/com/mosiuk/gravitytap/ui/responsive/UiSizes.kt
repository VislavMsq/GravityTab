package com.mosiuk.gravitytap.ui.responsive

import androidx.compose.material3.Typography
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class UiSizes(
    val isTablet: Boolean,
    val hPadding: Dp,
    val vSpacing: Dp,
    val controlHeight: Dp,
    val contentMaxWidth: Dp,
    val buttonWidthFraction: Float,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
)

@Composable
fun rememberUiSizes(
    window: WindowSizeClass,
    typography: Typography
): UiSizes = remember(window) {
    val tablet = window.widthSizeClass >= WindowWidthSizeClass.Medium
    UiSizes(
        isTablet = tablet,
        hPadding = if (tablet) 32.dp else 16.dp,
        vSpacing = if (tablet) 16.dp else 12.dp,
        controlHeight = if (tablet) 56.dp else 48.dp,
        contentMaxWidth = if (tablet) 720.dp else 420.dp,
        buttonWidthFraction = if (tablet) 0.72f else 0.6f,
        titleStyle = if (tablet) typography.headlineLarge else typography.headlineMedium,
        subtitleStyle = if (tablet) typography.titleLarge else typography.titleMedium
    )
}
