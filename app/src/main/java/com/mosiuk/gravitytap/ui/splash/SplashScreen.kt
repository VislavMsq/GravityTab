package com.mosiuk.gravitytap.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosiuk.gravitytap.R

@Composable
fun SplashScreen(
    vm: SplashViewModel,
    onDone: () -> Unit,
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.effects.collect { eff ->
            if (eff is SplashEffect.NavigateToMenu) onDone()
        }
    }

    Scaffold { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(spring(dampingRatio = 0.8f)),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                        ),
                )
            }
            Spacer(Modifier.height(24.dp))

            CircularProgressIndicator(progress = { ui.progress })
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.splash_loading))
        }
    }
}
