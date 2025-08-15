@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.mosiuk.gravitytap.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mosiuk.gravitytap.R
import com.mosiuk.gravitytap.domain.game.GameEffect
import com.mosiuk.gravitytap.ui.vm.GameUiEvent
import com.mosiuk.gravitytap.ui.vm.GameViewModel

@Composable
fun GameScreen(
    vm: GameViewModel,
    onFinish: (score: Int, difficulty: String, maxCombo: Int) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    // 1) Lifecycle-aware сбор UI-состояния
    val ui by vm.ui.collectAsStateWithLifecycle()

    // 2) Сбор одноразовых эффектов через repeatOnLifecycle(STARTED)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.effects.collect { eff ->
                if (eff is GameEffect.GameOver) {
                    onFinish(eff.score, eff.difficulty.name, eff.maxCombo)
                }
            }
        }
    }

    // Адаптив: простая эвристика для планшетов
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val topHPadding = if (isTablet) 32.dp else 12.dp

    // Размер шарика под ширину
    val ballSize: Dp = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 56.dp
        WindowWidthSizeClass.Medium -> 80.dp
        WindowWidthSizeClass.Expanded -> 96.dp
        else -> 72.dp
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = topHPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.score, ui.state.score),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.lives, ui.state.lives),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "   " + stringResource(R.string.combo, ui.state.combo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = { vm.onEvent(GameUiEvent.PauseToggle) }) {
                    Text(
                        if (ui.state.isPaused) stringResource(R.string.resume)
                        else stringResource(R.string.pause),
                    )
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val cellW = widthPx / 3f

            val ballSizePx = with(density) { ballSize.toPx() }
            val ballRadiusPx = ballSizePx / 2f

            // 3) Ground только из измерений контента (без топ-баров)
            val groundPx = heightPx - ballSizePx
            LaunchedEffect(groundPx) {
                if (groundPx > 0f) {
                    vm.onEvent(GameUiEvent.SetGround(groundPx = groundPx))
                }
            }

            val ball = ui.state.ball

            AnimatedVisibility(
                visible = ball != null,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
                exit = fadeOut(tween(100)),
            ) {
                if (ball != null) {
                    // 4) Координаты + защита от выхода за границы (clamp)
                    val centerXpx = (ball.column + 0.5f) * cellW
                    val leftXpxUnclamped = centerXpx - ballRadiusPx
                    val leftXpx = leftXpxUnclamped.coerceIn(0f, widthPx - ballSizePx)
                    val xDp = with(density) { leftXpx.toDp() }
                    val yDp = with(density) { ball.y.coerceIn(0f, groundPx).toDp() }

                    // 5) Без лишнего ripple/аллокаций при тапе
                    val interaction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .offset(x = xDp, y = yDp)
                            .size(ballSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                vm.onEvent(GameUiEvent.OnBallTap)
                            },
                    )
                }
            }
        }
    }
}
