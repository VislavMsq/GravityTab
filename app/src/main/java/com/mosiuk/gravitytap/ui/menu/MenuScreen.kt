package com.mosiuk.gravitytap.ui.menu

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mosiuk.gravitytap.R
import com.mosiuk.gravitytap.core.util.ClickThrottle
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.ui.vm.MenuEffect
import com.mosiuk.gravitytap.ui.vm.MenuEvent
import com.mosiuk.gravitytap.ui.vm.MenuViewModel
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
    // Получаем состояние UI из ViewModel
    val ui by vm.ui.collectAsState()
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding() // Учитываем системную панель навигации
            .padding(horizontal = hPadding), // Горизонтальные отступы
        contentAlignment = Alignment.Center // Выравнивание по центру
    ) {
        // Вертикальный контейнер для элементов меню
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth) // Ограничиваем максимальную ширину
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center, // Вертикальное выравнивание по центру
            horizontalAlignment = Alignment.CenterHorizontally, // Горизонтальное выравнивание по центру
        ) {
            // Заголовок меню
            Text(
                text = stringResource(R.string.menu_title), 
                style = titleStyle
            )

            // Отступ перед следующим элементом
            Spacer(Modifier.height(vGap))
            
            // Подзаголовок для выбора сложности
            Text(
                text = stringResource(R.string.select_difficulty), 
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
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
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
                    // Проверяем время с последнего нажатия
                    val now = SystemClock.uptimeMillis()
                    if (now - lastClickMs.longValue > 600L) {
                        lastClickMs.longValue = now
                        // Запускаем в корутине с троттлингом
                        scope.launch { 
                            if (throttle.allow()) vm.onEvent(MenuEvent.StartClicked) 
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction) // Ширина кнопки в процентах от ширины экрана
                    .height(controlH) // Высота кнопки
                    .widthIn(max = contentMaxWidth), // Максимальная ширина
            ) {
                Text(stringResource(R.string.start))
            }

            // Отступ между кнопками
            Spacer(Modifier.height(8.dp))
            
            // Кнопка перехода к таблице рекордов
            Button(
                onClick = onScores, // Вызываем переданный колбэк
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .height(controlH)
                    .widthIn(max = contentMaxWidth),
            ) {
                Text(stringResource(R.string.high_scores))
            }
        }
    }
}
