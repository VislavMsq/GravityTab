package com.mosiuk.gravitytap.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosiuk.gravitytap.R
import com.mosiuk.gravitytap.core.util.ClickThrottle
import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.ui.vm.ResultViewModel
import kotlinx.coroutines.launch

/**
 * Экран результатов игры, отображающий итоги последней игры и таблицу рекордов.
 * 
 * @param vm ViewModel экрана результатов, управляющий данными и логикой
 * @param windowSizeClass Класс размера окна для адаптивного дизайна
 * @param onPlayAgain Колбэк, вызываемый при нажатии кнопки "Играть снова"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    vm: ResultViewModel,
    windowSizeClass: WindowSizeClass,
    onPlayAgain: () -> Unit,
) {
    // Получаем состояние UI из ViewModel
    val ui = vm.ui
    val top by vm.top.collectAsState()

    // Настройка троттлинга для кнопки, чтобы избежать множественных нажатий
    val throttle = remember { ClickThrottle(windowMs = 600) }
    val scope = rememberCoroutineScope()

    // Адаптивный дизайн в зависимости от размера экрана
    val compactH = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    // Адаптивные размеры и отступы
    val hPadding = if (compactH) 12.dp else 16.dp
    val vGap = if (compactH) 8.dp else 16.dp
    val buttonH = if (compactH) 48.dp else 56.dp
    
    // Адаптивные пропорции для кнопок и контента
    val buttonWidthFraction = if (isTablet) 0.72f else if (compactH) 0.9f else 0.6f
    val contentMaxWidth = if (isTablet) 720.dp else 420.dp
    
    // Стили текста в зависимости от размера экрана
    val titleStyle =
        if (isTablet) MaterialTheme.typography.titleLarge
        else MaterialTheme.typography.titleMedium

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { inner ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = hPadding)
                    .widthIn(max = contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(vGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.score, ui.score),
                    style = titleStyle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.max_combo, ui.maxCombo),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { DifficultyChip(text = ui.difficulty.name) }

            item {
                Button(
                    onClick = {
                        scope.launch { if (throttle.allow()) onPlayAgain() }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth(buttonWidthFraction)
                            .height(buttonH),
                ) {
                    Text(stringResource(R.string.play_again))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.high_scores),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 8.dp))
            }

            items(top) { e -> ScoreCard(e) }
        }
    }
}

/**
 * Компонент чипа сложности, отображающий уровень сложности игры.
 * 
 * @param text Текст для отображения в чипе (название сложности)
 */
@Composable
private fun DifficultyChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors =
            AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Карточка с результатом в таблице рекордов.
 * 
 * @param e Запись с результатом игры
 */
@Composable
private fun ScoreCard(e: ScoreEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = e.score.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = stringResource(R.string.combo_x, e.maxCombo), style = MaterialTheme.typography.bodyMedium)
            DifficultyChip(text = e.difficulty)
        }
    }
}
