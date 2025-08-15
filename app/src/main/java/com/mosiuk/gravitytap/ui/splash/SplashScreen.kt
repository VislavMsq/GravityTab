package com.mosiuk.gravitytap.ui.splash

import androidx.compose.runtime.Composable

@Composable
fun SplashScreen(onLoaded: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoaded() }
}
