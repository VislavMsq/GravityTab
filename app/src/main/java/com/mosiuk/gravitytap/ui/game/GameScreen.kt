@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.mosiuk.gravitytap.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosiuk.gravitytap.R
import com.mosiuk.gravitytap.domain.game.GameEffect
import com.mosiuk.gravitytap.ui.vm.GameUiEvent
import com.mosiuk.gravitytap.ui.vm.GameViewModel

/**
 * Основной игровой экран, отображающий игровое поле, шарик и управляющие элементы.
 * 
 * @param vm ViewModel экрана игры, управляющий игровой логикой и состоянием
 * @param onFinish Колбэк, вызываемый при завершении игры с результатами
 * @param windowSizeClass Класс размера окна для адаптивного дизайна
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onFinish: (score: Int, difficulty: String, maxCombo: Int) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    // Собираем состояние UI из ViewModel
    val ui by vm.ui.collectAsState()

    // Обработка эффектов от ViewModel
    LaunchedEffect(Unit) {
        vm.effects.collect { eff ->
            // При получении эффекта завершения игры вызываем колбэк с результатами
            if (eff is GameEffect.GameOver) {
                onFinish(eff.score, eff.difficulty.name, eff.maxCombo)
            }
        }
    }

    // Адаптивный дизайн в зависимости от размера экрана
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    
    // Отступы и размеры элементов в зависимости от типа устройства
    val topHPadding = if (isTablet) 32.dp else 12.dp
    
    // Размер шарика адаптируется под размер экрана
    val ballSize: Dp =
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 56.dp    // Смартфоны
            WindowWidthSizeClass.Medium -> 80.dp     // Планшеты
            WindowWidthSizeClass.Expanded -> 96.dp   // Большие планшеты/складывающиеся
            else -> 72.dp                           // По умолчанию
        }

    // Основной каркас экрана с верхней панелью и игровым полем
    Scaffold(
        // Верхняя панель с очками, жизнями и кнопкой паузы
        topBar = {
            Row(
                modifier =
                    Modifier
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
                        if (ui.state.isPaused) {
                            stringResource(R.string.resume)
                        } else {
                            stringResource(R.string.pause)
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        // Контейнер с ограничениями для позиционирования игровых элементов
        // Используется для получения размеров экрана и позиционирования шарика
        BoxWithConstraints(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            // Получаем размеры контейнера в пикселях
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            
            // Вычисляем ширину ячейки (игровое поле разделено на 3 колонки)
            val cellW = widthPx / 3f

            // Преобразуем размер шарика в пиксели
            val ballSizePx = with(density) { ballSize.toPx() }
            val ballRadiusPx = ballSizePx / 2f

            LaunchedEffect(heightPx, ballSizePx) {
                vm.onEvent(GameUiEvent.SetGround(groundPx = heightPx - ballSizePx))
            }

            // Получаем текущий шарик из состояния
            val ball = ui.state.ball
            
            // Анимированное отображение шарика с эффектами появления/исчезновения
            AnimatedVisibility(
                visible = ball != null,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = .8f, animationSpec = tween(150)),
                exit = fadeOut(tween(100)),
            ) {
                if (ball != null) {
                    // Вычисляем координаты для отрисовки шарика
                    val centerXpx = (ball.column + 0.5f) * cellW  // Центр по X в пикселях
                    val leftXpx = centerXpx - ballRadiusPx         // Левый край шарика
                    val xDp = with(density) { leftXpx.toDp() }     // X в dp для Compose
                    val yDp = with(density) { ball.y.toDp() }      // Y в dp для Compose

                    // Отрисовываем шарик как нажимаемый круг
                    Box(
                        modifier =
                            Modifier
                                .offset(x = xDp, y = yDp)  // Позиционируем шарик
                                .size(ballSize)            // Устанавливаем размер
                                .clip(CircleShape)         // Обрезаем по кругу
                                .background(MaterialTheme.colorScheme.error)  // Красный цвет из темы
                                .clickable { 
                                    // Обработка нажатия на шарик
                                    vm.onEvent(GameUiEvent.OnBallTap) 
                                },
                    )
                }
            }
        }
    }
}
