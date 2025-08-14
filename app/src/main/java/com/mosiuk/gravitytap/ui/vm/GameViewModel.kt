package com.mosiuk.gravitytap.ui.vm

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import com.mosiuk.gravitytap.domain.game.GameAction
import com.mosiuk.gravitytap.domain.game.GameEffect
import com.mosiuk.gravitytap.domain.game.GameLoop
import com.mosiuk.gravitytap.domain.game.GameReducer
import com.mosiuk.gravitytap.domain.game.SpawnScheduler
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.model.GameState
import com.mosiuk.gravitytap.domain.usecase.SpawnBallUseCase
import com.mosiuk.gravitytap.domain.usecase.TickPhysicsUseCase
import com.mosiuk.gravitytap.domain.usecase.UpdateScoreOnHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val state: GameState,
    val timeMs: Long = 0L
)

sealed interface GameUiEvent {
    data object PauseToggle : GameUiEvent;
    data object OnBallTap : GameUiEvent
}

@HiltViewModel
class GameViewModel(
    private val handle: SavedStateHandle,
    private val spawnUC: SpawnBallUseCase,
    tickUC: TickPhysicsUseCase,
    scoreUC: UpdateScoreOnHitUseCase,
    private val dispatchers: DispatchersProvider
) : ViewModel() {

    private val reducer = GameReducer(tick = tickUC, spawn = spawnUC, score = scoreUC)

    private var state: GameState
        get() = handle["state"] ?: GameState(Difficulty.NORMAL)
        set(value) {
            handle["state"] = value
        }

    private val scheduler = SpawnScheduler(handle["nextSpawnAt"] ?: SystemClock.uptimeMillis())

    private val _ui = MutableStateFlow(GameUiState(state))

    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _effects = Channel<GameEffect>(Channel.BUFFERED)

    val effect: Flow<GameEffect> = _effects.receiveAsFlow()
    private val loop = GameLoop(frameMs = 16L)

    init {
        viewModelScope.launch(dispatchers.main) {
            loop.ticks().collect { tick ->
                onAction(tick)

                val d = state.difficulty
                if (state.ball == null && scheduler.shouldSpawn(tick.nowMs, d.spawnsMs)) {
                    onAction(GameAction.Spawn(tick.nowMs))
                }
            }
        }
    }

    fun onEvent(e: GameUiEvent) {
        when (e) {
            GameUiEvent.PauseToggle -> onAction(GameAction.PauseToggle)
            GameUiEvent.OnBallTap -> onAction(GameAction.Tap)
        }
    }

    private fun onAction(a: GameAction) {
        val (newState, effect) = reducer.reduce(state, a)

        if (newState != state) {
            state = newState
            handle["nextSpawnAt"] = scheduler.snapshot()
            _ui.value =
                _ui.value.copy(state = newState, timeMs = (a as? GameAction.Tick)?.nowMs ?: 0L)
        }
        if (effect != null) {
            viewModelScope.launch {
                _effects.send(effect)
            }
        }
    }
}

