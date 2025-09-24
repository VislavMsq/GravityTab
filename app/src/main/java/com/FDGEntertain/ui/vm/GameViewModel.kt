package com.FDGEntertain.ui.vm

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FDGEntertain.core.util.DispatchersProvider
import com.FDGEntertain.core.util.SoundManager
import com.FDGEntertain.data.datastore.SettingsDataStore
import com.FDGEntertain.domain.game.GameAction
import com.FDGEntertain.domain.game.GameEffect
import com.FDGEntertain.domain.game.GameLoop
import com.FDGEntertain.domain.game.GameReducer
import com.FDGEntertain.domain.game.SpawnScheduler
import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.domain.model.GameState
import com.FDGEntertain.domain.usecase.SpawnBallUseCase
import com.FDGEntertain.domain.usecase.TickPhysicsUseCase
import com.FDGEntertain.domain.usecase.UpdateScoreOnHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.map                   // ⬅️ NEW
import kotlinx.coroutines.flow.distinctUntilChanged // ⬅️ NEW
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val state: GameState,
    val timeMs: Long = 0L,
)

sealed interface GameUiEvent {
    data object PauseToggle : GameUiEvent
    data object OnBallTap : GameUiEvent
    data class SetGround(val groundPx: Float) : GameUiEvent
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val spawnUC: SpawnBallUseCase,
    private val tickUC: TickPhysicsUseCase,
    private val scoreUC: UpdateScoreOnHitUseCase,
    private val dispatchers: DispatchersProvider,
    private val settings: SettingsDataStore,
    private val sound: SoundManager,
) : ViewModel() {

    private companion object {
        const val KEY_STATE = "state"
        const val KEY_NEXT_SPAWN = "nextSpawnAt"
        const val KEY_ARG_DIFFICULTY = "difficulty"
    }

    @Volatile
    private var soundEnabled: Boolean = true
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        sound.setMuted(!enabled)
        if (!enabled) sound.stopAll()
    }

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
        set(value) { handle[KEY_STATE] = value }

    private val scheduler =
        SpawnScheduler(handle.get<Long>(KEY_NEXT_SPAWN) ?: SystemClock.uptimeMillis())

    private val _ui = MutableStateFlow(GameUiState(state))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _effects = Channel<GameEffect>(Channel.BUFFERED)
    val effects: Flow<GameEffect> = _effects.receiveAsFlow()

    private val loop = GameLoop(frameMs = 16L)

    init {
        viewModelScope.launch {
            settings.settings
                .map { it.second }
                .distinctUntilChanged()
                .collect { on -> setSoundEnabled(on) }
        }

        // Игровой цикл
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
                reducer = GameReducer(
                    tick = tickUC,
                    spawn = spawnUC,
                    score = scoreUC,
                    groundY = e.groundPx,
                )
            }
        }
    }

    private fun onAction(action: GameAction) {
        val prevState = state
        val (newState, effect) = reducer.reduce(prevState, action)

        if (newState != prevState) {
            state = newState
            handle[KEY_NEXT_SPAWN] = scheduler.snapshot()
            _ui.value = _ui.value.copy(
                state = newState,
                timeMs = (action as? GameAction.Tick)?.nowMs ?: _ui.value.timeMs,
            )
        }

        val wasHit = newState.score > prevState.score
        val wasMiss = newState.lives < prevState.lives
        when {
            wasHit  -> sound.playHit(soundEnabled)
            wasMiss -> sound.playMiss(soundEnabled)
        }

        if (effect != null) {
            viewModelScope.launch { _effects.send(effect) }
        }
    }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }
}
