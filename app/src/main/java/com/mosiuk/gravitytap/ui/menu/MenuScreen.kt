package com.mosiuk.gravitytap.ui.menu

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.ui.vm.MenuEffect
import com.mosiuk.gravitytap.ui.vm.MenuEvent
import com.mosiuk.gravitytap.ui.vm.MenuViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MenuScreen(
    onStart: (String) -> Unit,
    onScores: () -> Unit,
    vm: MenuViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.effects.collectLatest { eff ->
            if (eff is MenuEffect.NavigateToGame) onStart(eff.difficulty.name)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Gravity Tap",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text(text = "Difficulty")

        val options = listOf(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, diff ->
                SegmentedButton(
                    selected = ui.difficulty == diff,
                    onClick = { vm.onEvent(MenuEvent.SelectDifficulty(diff)) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                    label = { Text(diff.name) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(text = "Sound")
        Switch(
            checked = ui.sound,
            onCheckedChange = { vm.onEvent(MenuEvent.ToggleSound(it)) },
        )

        Spacer(Modifier.height(24.dp))

        // простой анти-дабл-клик
        val lastClickMs = remember { mutableLongStateOf(0L) }
        Button(
            onClick = {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickMs.longValue > 600L) {
                    lastClickMs.longValue = now
                    vm.onEvent(MenuEvent.StartClicked)
                }
            },
            contentPadding =
                PaddingValues(
                    horizontal = 32.dp,
                    vertical = 12.dp,
                ),
        ) {
            Text("Start")
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onScores) { Text("High Scores") }
    }
}
