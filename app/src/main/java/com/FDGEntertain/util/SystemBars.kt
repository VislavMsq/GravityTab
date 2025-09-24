package com.FDGEntertain.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ApplySystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkStatusIcons: Boolean,
    darkNavIcons: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)

        val prevStatus = window.statusBarColor
        val prevNav = window.navigationBarColor
        val prevLightStatus = controller.isAppearanceLightStatusBars
        val prevLightNav = controller.isAppearanceLightNavigationBars

        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
        controller.isAppearanceLightStatusBars = darkStatusIcons   // true = тёмные иконки
        controller.isAppearanceLightNavigationBars = darkNavIcons

        onDispose {
            window.statusBarColor = prevStatus
            window.navigationBarColor = prevNav
            controller.isAppearanceLightStatusBars = prevLightStatus
            controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }
}
