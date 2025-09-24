package com.FDGEntertain.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FDGEntertain.R
import com.FDGEntertain.core.util.ClickThrottle
import com.FDGEntertain.domain.model.ScoreEntry
import com.FDGEntertain.ui.vm.ResultViewModel
import com.FDGEntertain.util.ApplySystemBars
import kotlinx.coroutines.launch

/**
 * Экран результатов игры, отображающий итоги последней игры и таблицу рекордов.
 */
@Composable
fun ResultScreen(
    vm: ResultViewModel,
    windowSizeClass: WindowSizeClass,
    onPlayAgain: () -> Unit,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ApplySystemBars(
        statusBarColor = if (dark) Color.Black else Color.White,
        navigationBarColor = if (dark) Color.Black else Color.White,
        darkStatusIcons = !dark,
        darkNavIcons = !dark
    )

    val ui = vm.ui
    val top by vm.top.collectAsState()
    val throttle = remember { ClickThrottle(windowMs = 600) }
    val scope = rememberCoroutineScope()

    val compactH = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val hPadding = if (isTablet) 32.dp else 16.dp
    val vGap = if (compactH) 8.dp else 16.dp
    val buttonH = if (compactH) 48.dp else 56.dp

    val buttonWidthFraction = if (isTablet) 0.72f else if (compactH) 0.9f else 0.6f
    val contentMaxWidth = if (isTablet) 720.dp else 420.dp

    val titleStyle = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium
    val subtitleStyle = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
    val btnShape = RoundedCornerShape(28.dp)

    // Используем такую же структуру как в MenuScreen
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .paint(painterResource(R.drawable.image6), contentScale = ContentScale.Crop)
                .padding(horizontal = hPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(vGap),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Заголовок экрана как первый элемент
                item {
                    Text(
                        text = stringResource(R.string.result_title),
                        color = Color.White,
                        style = titleStyle,
                        modifier = Modifier.padding(bottom = vGap)
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.score, ui.score),
                        color = Color.White,
                        style = subtitleStyle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.max_combo, ui.maxCombo),
                        color = Color.White,
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
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = btnShape,
                        modifier = Modifier
                            .fillMaxWidth(buttonWidthFraction)
                            .height(buttonH)
                            .clip(btnShape)
                            .paint(
                                painterResource(R.drawable.image3),
                                contentScale = ContentScale.FillBounds
                            ),
                    ) {
                        Text(stringResource(R.string.play_again))
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.high_scores),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    HorizontalDivider(
                        Modifier.padding(top = 8.dp, bottom = 8.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                items(top) { e -> ScoreCard(e) }
            }
        }
    }
}

/**
 * Компонент чипа сложности, отображающий уровень сложности игры.
 */
@Composable
private fun DifficultyChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Карточка с результатом в таблице рекордов.
 */
@Composable
private fun ScoreCard(e: ScoreEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
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
