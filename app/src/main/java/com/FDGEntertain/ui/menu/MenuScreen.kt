package com.FDGEntertain.ui.menu

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.FDGEntertain.R
import com.FDGEntertain.core.util.ClickThrottle
import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.ui.vm.MenuEffect
import com.FDGEntertain.ui.vm.MenuEvent
import com.FDGEntertain.ui.vm.MenuViewModel
import com.FDGEntertain.util.ApplySystemBars
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Возвращает строковый ресурс для отображения названия сложности.
 *
 * @return Идентификатор строкового ресурса с названием сложности
 */
@StringRes
private fun Difficulty.titleRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_label_easy
    Difficulty.NORMAL -> R.string.difficulty_label_normal
    Difficulty.HARD -> R.string.difficulty_label_hard
}

/**
 * Главный экран меню приложения, содержащий настройки и кнопки навигации.
 *
 * @param windowSizeClass Класс размера окна для адаптивного дизайна
 * @param onStart Колбэк, вызываемый при нажатии кнопки начала игры
 * @param onScores Колбэк, вызываемый при нажатии кнопки перехода к таблице рекордов
 * @param vm ViewModel экрана меню, управляющий состоянием и логикой
 */
@Composable
fun MenuScreen(
    windowSizeClass: WindowSizeClass,
    onStart: (String) -> Unit,
    onScores: () -> Unit,
    vm: MenuViewModel = hiltViewModel(),
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ApplySystemBars(
        statusBarColor = if (dark) Color.Black else Color.White,
        navigationBarColor = if (dark) Color.Black else Color.White,
        darkStatusIcons = !dark,
        darkNavIcons = !dark
    )

    // Получаем состояние UI из ViewModel
    val ui by vm.ui.collectAsStateWithLifecycle()
    // Инициализируем троттлинг для предотвращения множественных нажатий
    val throttle = remember { ClickThrottle(windowMs = 600) }
    // Корутин-скоп для асинхронных операций
    val scope = rememberCoroutineScope()

    // Адаптивный дизайн в зависимости от размера экрана
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    // Настройки отступов и размеров в зависимости от типа устройства
    val hPadding = if (isTablet) 32.dp else 16.dp
    val vGap = if (isTablet) 16.dp else 12.dp
    val controlH = if (isTablet) 56.dp else 48.dp
    val buttonWidthFraction = if (isTablet) 0.72f else 0.6f
    val contentMaxWidth = if (isTablet) 720.dp else 420.dp

    // Стили текста в зависимости от типа устройства
    val titleStyle =
        if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium
    val subtitleStyle =
        if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium

    // Обработка эффектов от ViewModel
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            // При получении эффекта навигации в игру вызываем колбэк onStart
            if (effect is MenuEffect.NavigateToGame) onStart(effect.difficulty.name)
        }
    }

    // Основной контейнер экрана
    Box(                                      // 1) корень красим в белый
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(                                  // 2) контентная зона = экран минус system bars
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .paint(painterResource(R.drawable.image6), contentScale = ContentScale.Crop)
                .padding(horizontal = hPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Заголовок меню
                Text(
                    text = stringResource(R.string.menu_title),
                    color = Color.White,
                    style = titleStyle
                )

                // Отступ перед следующим элементом
                Spacer(Modifier.height(vGap))

                // Подзаголовок для выбора сложности
                Text(
                    text = stringResource(R.string.select_difficulty),
                    color = Color.White,
                    style = subtitleStyle
                )

                // Отступ перед переключателем сложности
                Spacer(Modifier.height(8.dp))

                // Горизонтальный ряд кнопок для выбора сложности
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth()
                ) {
                    val options = listOf(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)
                    options.forEachIndexed { index, diff ->
                        SegmentedButton(
                            selected = ui.difficulty == diff,
                            onClick = {
                                // Отправляем событие выбора сложности в ViewModel
                                vm.onEvent(MenuEvent.SelectDifficulty(diff))
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            modifier = Modifier.height(controlH),
                            // Отображаем локализованное название сложности
                            label = { Text(stringResource(diff.titleRes())) },
                        )
                    }
                }

                // Отступ перед настройкой звука
                Spacer(Modifier.height(vGap))

                // Подзаголовок для настройки звука
                Text(
                    text = stringResource(R.string.sound),
                    color = Color.White,
                    style = subtitleStyle
                )

                // Переключатель звука
                Switch(
                    checked = ui.sound,
                    onCheckedChange = {
                        // Отправляем событие переключения звука в ViewModel
                        vm.onEvent(MenuEvent.ToggleSound(it))
                    },
                    modifier = Modifier.height(controlH),
                )

                // Увеличенный отступ перед кнопками
                Spacer(Modifier.height(vGap * 1.5f))

                // Таймер для предотвращения двойных нажатий
                val lastClickMs = remember { mutableLongStateOf(0L) }

                // Кнопка начала игры
                Button(
                    onClick = {
                        val now = SystemClock.uptimeMillis()
                        if (now - lastClickMs.longValue > 600L) {
                            lastClickMs.longValue = now
                            scope.launch { if (throttle.allow()) vm.onEvent(MenuEvent.StartClicked) }
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(buttonWidthFraction)
                        .height(controlH)
                        .widthIn(max = contentMaxWidth)
                        .clip(RoundedCornerShape(28.dp))
                        .paint(
                            painterResource(R.drawable.image3), // или тот же, что сверху
                            contentScale = ContentScale.FillBounds
                        ),
                ) {
                    Text(stringResource(R.string.start))
                }

                // Отступ между кнопками
                Spacer(Modifier.height(8.dp))

                // Кнопка перехода к таблице рекордов
                // Кнопка начала игры
                Button(
                    onClick = onScores,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(buttonWidthFraction)
                        .height(controlH)
                        .widthIn(max = contentMaxWidth)
                        .clip(RoundedCornerShape(28.dp))
                        .paint(
                            painterResource(R.drawable.image3), // или тот же, что сверху
                            contentScale = ContentScale.FillBounds
                        ),
                ) {
                    Text(stringResource(R.string.high_scores))
                }
            }
        }
    }
}
