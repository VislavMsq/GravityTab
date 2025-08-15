@file:OptIn(ExperimentalAnimationApi::class)

package com.mosiuk.gravitytap.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosiuk.gravitytap.domain.game.GameEffect
import com.mosiuk.gravitytap.ui.vm.GameUiEvent
import com.mosiuk.gravitytap.ui.vm.GameViewModel

@Composable
fun GameScreen(
    vm: GameViewModel,
    onFinish: (score: Int, difficulty: String, maxCombo: Int) -> Unit,
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.effects.collect { eff ->
            when (eff) {
                is GameEffect.GameOver -> onFinish(eff.score, eff.difficulty.name, eff.maxCombo)
            }
        }
    }

    val ballSize: Dp = 72.dp

    Scaffold(
        topBar = {
            Row(
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Score: ${ui.state.score}  Lives: ${ui.state.lives}  Combo: ${ui.state.combo}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { vm.onEvent(GameUiEvent.PauseToggle) }) {
                    Text(if (ui.state.isPaused) "Resume" else "Pause")
                }
            }
        },
    ) { inner ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
        ) {
            // 1) density нужен для toPx()/toDp()
            val density = LocalDensity.current

            // 2) размеры контейнера в px
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            val cellW = widthPx / 3f

            // 3) диаметр и радиус шарика в px
            val ballSizePx = with(density) { ballSize.toPx() }
            val ballRadiusPx = ballSizePx / 2f

            // если у тебя есть событие установки «земли»
            LaunchedEffect(heightPx, ballSizePx) {
                vm.onEvent(GameUiEvent.SetGround(groundPx = heightPx - ballSizePx))
            }

            // ... Canvas с сеткой
            Canvas(Modifier.fillMaxSize()) {
                val gridColor = Color.Black.copy(alpha = 0.05f)
                val stroke = with(density) { 1.dp.toPx() }
                drawLine(gridColor, Offset(cellW, 0f), Offset(cellW, size.height), strokeWidth = stroke)
                drawLine(gridColor, Offset(cellW * 2, 0f), Offset(cellW * 2, size.height), strokeWidth = stroke)
                drawLine(gridColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
            }

            val ball = ui.state.ball
            AnimatedVisibility(
                visible = ball != null,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = .8f, animationSpec = tween(150)),
                exit = fadeOut(tween(100)),
            ) {
                if (ball != null) {
                    val centerXpx = (ball.column + 0.5f) * cellW
                    val leftXpx = centerXpx - ballRadiusPx

                    // 4) конвертация px → dp для Modifier.offset(Dp, Dp)
                    val xDp = with(density) { leftXpx.toDp() }
                    val yDp = with(density) { ball.y.toDp() }

                    Box(
                        modifier =
                            Modifier
                                .offset(x = xDp, y = yDp)
                                .size(ballSize) // 72.dp
                                .clip(CircleShape)
                                .background(Color.Red)
                                .clickable { vm.onEvent(GameUiEvent.OnBallTap) },
                    )
                }
            }
        }
    }
}
