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
import javax.inject.Inject

data class GameUiState(
    val state: GameState,
    val timeMs: Long = 0L,
)

sealed interface GameUiEvent {
    data object PauseToggle : GameUiEvent

    data object OnBallTap : GameUiEvent

    data class SetGround(
        val groundPx: Float,
    ) : GameUiEvent
}

@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val spawnUC: SpawnBallUseCase,
        private val tickUC: TickPhysicsUseCase, // ← ТЕПЕРЬ это поля класса
        private val scoreUC: UpdateScoreOnHitUseCase, // ← ТЕПЕРЬ это поля класса
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        private companion object {
            const val KEY_STATE = "state"
            const val KEY_NEXT_SPAWN = "nextSpawnAt"
            const val KEY_ARG_DIFFICULTY = "difficulty"
        }

        // можно безопасно пересоздавать при изменении ground
        private var reducer =
            GameReducer(
                tick = tickUC,
                spawn = spawnUC,
                score = scoreUC,
                groundY = 1000f,
            )

        private var state: GameState
            get() {
                handle.get<GameState>(KEY_STATE)?.let { return it }
                val arg = handle.get<String>(KEY_ARG_DIFFICULTY) ?: Difficulty.NORMAL.name
                val diff = runCatching { Difficulty.valueOf(arg) }.getOrDefault(Difficulty.NORMAL)
                return GameState(diff)
            }
            set(value) {
                handle[KEY_STATE] = value
            }

        private val scheduler =
            SpawnScheduler(handle.get<Long>(KEY_NEXT_SPAWN) ?: SystemClock.uptimeMillis())

        private val _ui = MutableStateFlow(GameUiState(state))
        val ui: StateFlow<GameUiState> = _ui.asStateFlow()

        private val _effects = Channel<GameEffect>(Channel.BUFFERED)
        val effects: Flow<GameEffect> = _effects.receiveAsFlow()

        private val loop = GameLoop(frameMs = 16L)

        init {
            viewModelScope.launch(dispatchers.main) {
                loop.ticks().collect { tick ->
                    onAction(tick)

                    val d = state.difficulty
                    if (state.ball == null && scheduler.shouldSpawn(tick.nowMs, d.spawnMs)) {
                        onAction(GameAction.Spawn(tick.nowMs))
                    }
                }
            }
        }

        fun onEvent(e: GameUiEvent) {
            when (e) {
                GameUiEvent.PauseToggle -> onAction(GameAction.PauseToggle)
                GameUiEvent.OnBallTap -> onAction(GameAction.Tap)
                is GameUiEvent.SetGround -> {
                    // пересоздаём редьюсер с новым ground
                    reducer =
                        GameReducer(
                            tick = tickUC,
                            spawn = spawnUC,
                            score = scoreUC,
                            groundY = e.groundPx,
                        )
                }
            }
        }

        private fun onAction(a: GameAction) {
            val (newState, effect) = reducer.reduce(state, a)
            if (newState != state) {
                state = newState
                handle[KEY_NEXT_SPAWN] = scheduler.snapshot() // ← используем константу
                _ui.value =
                    _ui.value.copy(
                        state = newState,
                        timeMs = (a as? GameAction.Tick)?.nowMs ?: _ui.value.timeMs,
                    )
            }
            if (effect != null) {
                viewModelScope.launch { _effects.send(effect) }
            }
        }
    }
