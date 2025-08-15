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

/**
 * Состояние UI игрового экрана.
 * 
 * @property state Текущее состояние игры
 * @property timeMs Текущее время игры в миллисекундах
 */
data class GameUiState(
    val state: GameState,
    val timeMs: Long = 0L,
)

/**
 * События пользовательского интерфейса игрового экрана.
 */
sealed interface GameUiEvent {
    /** Событие переключения паузы */
    data object PauseToggle : GameUiEvent

    /** Событие нажатия на шарик */
    data object OnBallTap : GameUiEvent

    /**
     * Событие установки позиции "земли" (нижней границы игрового поля).
     * 
     * @property groundPx Позиция земли в пикселях от верха экрана
     */
    data class SetGround(
        val groundPx: Float,
    ) : GameUiEvent
}

/**
 * ViewModel игрового экрана, управляющая состоянием и логикой игры.
 * 
 * @property handle SavedStateHandle для сохранения состояния
 * @property spawnUC UseCase для создания новых шариков
 * @property tickUC UseCase для обновления физики игры
 * @property scoreUC UseCase для обновления счета
 * @property dispatchers Провайдер корутин-диспетчеров
 */
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val spawnUC: SpawnBallUseCase,
        private val tickUC: TickPhysicsUseCase,
        private val scoreUC: UpdateScoreOnHitUseCase,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        private companion object {
            // Ключи для сохранения состояния
            const val KEY_STATE = "state"
            const val KEY_NEXT_SPAWN = "nextSpawnAt"
            const val KEY_ARG_DIFFICULTY = "difficulty"
        }

        /**
         * Редьюсер, управляющий обновлением состояния игры.
         * Может быть безопасно пересоздан при изменении позиции земли.
         */
        private var reducer =
            GameReducer(
                tick = tickUC,
                spawn = spawnUC,
                score = scoreUC,
                groundY = 1000f,
            )

        /**
         * Текущее состояние игры с геттером и сеттером, сохраняющим состояние.
         * При первом обращении инициализируется из аргументов навигации.
         */
        private var state: GameState
            get() {
                // Пытаемся восстановить сохраненное состояние
                handle.get<GameState>(KEY_STATE)?.let { return it }
                
                // Если состояние не сохранено, создаем новое на основе сложности
                val arg = handle.get<String>(KEY_ARG_DIFFICULTY) ?: Difficulty.NORMAL.name
                val diff = runCatching { Difficulty.valueOf(arg) }.getOrDefault(Difficulty.NORMAL)
                return GameState(diff)
            }
            set(value) {
                // Сохраняем новое состояние
                handle[KEY_STATE] = value
            }

        // Планировщик появления новых шариков
        private val scheduler =
            SpawnScheduler(handle.get<Long>(KEY_NEXT_SPAWN) ?: SystemClock.uptimeMillis())

        // Поток состояния UI
        private val _ui = MutableStateFlow(GameUiState(state))
        val ui: StateFlow<GameUiState> = _ui.asStateFlow()

        // Канал для отправки эффектов (навигация, звуки и т.д.)
        private val _effects = Channel<GameEffect>(Channel.BUFFERED)
        val effects: Flow<GameEffect> = _effects.receiveAsFlow()

        // Игровой цикл с частотой ~60 FPS (1000ms / 60 ≈ 16ms на кадр)
        private val loop = GameLoop(frameMs = 16L)

        init {
            // Запускаем игровой цикл
            viewModelScope.launch(dispatchers.main) {
                loop.ticks().collect { tick ->
                    // Обновляем физику игры на каждом тике
                    onAction(tick)

                    // Проверяем, нужно ли создать новый шарик
                    val d = state.difficulty
                    if (state.ball == null && scheduler.shouldSpawn(tick.nowMs, d.spawnMs)) {
                        onAction(GameAction.Spawn(tick.nowMs))
                    }
                }
            }
        }

        /**
         * Обработчик событий от пользовательского интерфейса.
         * 
         * @param e Событие от UI
         */
        fun onEvent(e: GameUiEvent) {
            when (e) {
                // Обработка нажатия на кнопку паузы
                GameUiEvent.PauseToggle -> onAction(GameAction.PauseToggle)
                
                // Обработка нажатия на шарик
                GameUiEvent.OnBallTap -> onAction(GameAction.Tap)
                
                // Обработка изменения позиции земли (при изменении размера экрана)
                is GameUiEvent.SetGround -> {
                    // Пересоздаем редьюсер с новой позицией земли
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

        /**
         * Обработчик игровых действий, обновляющий состояние игры.
         * 
         * @param a Действие для обработки
         */
        private fun onAction(a: GameAction) {
            // Получаем новое состояние и эффект из редьюсера
            val (newState, effect) = reducer.reduce(state, a)
            
            // Если состояние изменилось, обновляем UI
            if (newState != state) {
                // Сохраняем текущее состояние
                state = newState
                handle[KEY_NEXT_SPAWN] = scheduler.snapshot()
                
                // Обновляем UI с новым состоянием
                _ui.value =
                    _ui.value.copy(
                        state = newState,
                        // Обновляем время только для тиков, для остальных действий оставляем прежнее
                        timeMs = (a as? GameAction.Tick)?.nowMs ?: _ui.value.timeMs,
                    )
            }
            
            // Если есть эффект, отправляем его в канал
            if (effect != null) {
                viewModelScope.launch { _effects.send(effect) }
            }
        }
    }
