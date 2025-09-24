package com.FDGEntertain.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.FDGEntertain.R
import com.FDGEntertain.domain.game.GameEffect
import com.FDGEntertain.ui.vm.GameUiEvent
import com.FDGEntertain.ui.vm.GameViewModel
import com.FDGEntertain.util.ApplySystemBars

@Composable
fun GameScreen(
    vm: GameViewModel,
    onFinish: (score: Int, difficulty: String, maxCombo: Int) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ApplySystemBars(
        statusBarColor = if (dark) Color.Black else Color.White,
        navigationBarColor = if (dark) Color.Black else Color.White,
        darkStatusIcons = !dark,
        darkNavIcons = !dark
    )

    val ui by vm.ui.collectAsStateWithLifecycle()
    val pauseShape = RoundedCornerShape(28.dp)

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.effects.collect { eff ->
                if (eff is GameEffect.GameOver) onFinish(eff.score, eff.difficulty.name, eff.maxCombo)
            }
        }
    }

    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val topHPadding = if (isTablet) 32.dp else 12.dp

    val ballSize: Dp = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 56.dp
        WindowWidthSizeClass.Medium -> 80.dp
        WindowWidthSizeClass.Expanded -> 96.dp
        else -> 72.dp
    }

    // Используем такую же структуру как в MenuScreen и ResultScreen
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .paint(
                    painterResource(R.drawable.image6),
                    contentScale = ContentScale.Crop
                )
        ) {
            // Игровая панель поверх картинки
            Row(
                modifier = Modifier
                    .padding(horizontal = topHPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.score, ui.state.score),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.lives, ui.state.lives),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "   " + stringResource(R.string.combo, ui.state.combo),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Button(
                    onClick = { vm.onEvent(GameUiEvent.PauseToggle) },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = pauseShape,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(pauseShape)
                        .paint(
                            painterResource(R.drawable.image3),
                            contentScale = ContentScale.FillBounds
                        ),
                ) {
                    Text(stringResource(R.string.pause))
                }
            }

            // Игровая область
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp) // Отступ для игровой панели
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val cellW = widthPx / 3f

                val ballSizePx = with(density) { ballSize.toPx() }
                val ballRadiusPx = ballSizePx / 2f

                val groundPx = heightPx - ballSizePx
                LaunchedEffect(groundPx) {
                    if (groundPx > 0f) vm.onEvent(GameUiEvent.SetGround(groundPx))
                }

                val ball = ui.state.ball
                AnimatedVisibility(
                    visible = ball != null,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
                    exit = fadeOut(tween(100)),
                ) {
                    if (ball != null) {
                        val centerXpx = (ball.column + 0.5f) * cellW
                        val leftXpx = (centerXpx - ballRadiusPx).coerceIn(0f, widthPx - ballSizePx)
                        val xDp = with(density) { leftXpx.toDp() }
                        val yDp = with(density) { ball.y.coerceIn(0f, groundPx).toDp() }
                        val interaction = remember { MutableInteractionSource() }

                        Image(
                            painter = painterResource(R.drawable.image13),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = xDp, y = yDp)
                                .size(ballSize)
                                .clickable(interactionSource = interaction, indication = null) {
                                    vm.onEvent(GameUiEvent.OnBallTap)
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}
