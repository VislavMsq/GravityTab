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

@StringRes
private fun Difficulty.titleRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_label_easy
    Difficulty.NORMAL -> R.string.difficulty_label_normal
    Difficulty.HARD -> R.string.difficulty_label_hard
}

@Composable
fun MenuScreen(
    windowSizeClass: WindowSizeClass,
    onStart: (String) -> Unit,
    onScores: () -> Unit,
    vm: MenuViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val throttle = remember { ClickThrottle(windowMs = 600) }
    val scope = rememberCoroutineScope()

    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val hPadding = if (isTablet) 32.dp else 16.dp
    val vGap = if (isTablet) 16.dp else 12.dp
    val controlH = if (isTablet) 56.dp else 48.dp
    val buttonWidthFraction = if (isTablet) 0.72f else 0.6f
    val contentMaxWidth = if (isTablet) 720.dp else 420.dp
    val titleStyle =
        if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium
    val subtitleStyle =
        if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium

    LaunchedEffect(Unit) {
        vm.effects.collectLatest { eff ->
            if (eff is MenuEffect.NavigateToGame) onStart(eff.difficulty.name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
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
            Text(text = stringResource(R.string.menu_title), style = titleStyle)

            Spacer(Modifier.height(vGap))
            Text(text = stringResource(R.string.select_difficulty), style = subtitleStyle)

            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
            ) {
                val options = listOf(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)
                options.forEachIndexed { index, diff ->
                    SegmentedButton(
                        selected = ui.difficulty == diff,
                        onClick = { vm.onEvent(MenuEvent.SelectDifficulty(diff)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.height(controlH),
                        // ✅ было: Text(diff.name)
                        label = { Text(stringResource(diff.titleRes())) },
                    )
                }
            }

            Spacer(Modifier.height(vGap))
            Text(text = stringResource(R.string.sound), style = subtitleStyle)
            Switch(
                checked = ui.sound,
                onCheckedChange = { vm.onEvent(MenuEvent.ToggleSound(it)) },
                modifier = Modifier.height(controlH),
            )

            Spacer(Modifier.height(vGap * 1.5f))

            val lastClickMs = remember { mutableLongStateOf(0L) }
            Button(
                onClick = {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastClickMs.longValue > 600L) {
                        lastClickMs.longValue = now
                        scope.launch { if (throttle.allow()) vm.onEvent(MenuEvent.StartClicked) }
                    }
                },
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .height(controlH)
                    .widthIn(max = contentMaxWidth),
            ) {
                Text(stringResource(R.string.start))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onScores,
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
