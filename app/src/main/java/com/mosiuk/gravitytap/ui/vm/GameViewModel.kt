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
 * Пакет содержит ViewModel'и экранов приложения.
 * 
 * ViewModel'и отвечают за хранение и обработку данных, связанных с UI,
 * а также за бизнес-логику приложения.
 */

/**
 * Класс, представляющий состояние пользовательского интерфейса игрового экрана.
 * 
 * @property state Текущее состояние игрового процесса, включая счет, жизни и другие игровые параметры.
 * @property timeMs Текущее время игры в миллисекундах, используется для анимаций и таймеров.
 */
data class GameUiState(
    val state: GameState,
    val timeMs: Long = 0L,
)

/**
 * Запечатанный интерфейс, представляющий события пользовательского интерфейса игрового экрана.
 * Используется для обработки пользовательского ввода и других UI-событий.
 */
sealed interface GameUiEvent {
    /** 
     * Событие переключения состояния паузы.
     * Вызывается при нажатии на кнопку паузы/продолжения игры.
     */
    data object PauseToggle : GameUiEvent

    /** 
     * Событие нажатия на шарик.
     * Вызывается, когда пользователь нажимает на активный шарик.
     */
    data object OnBallTap : GameUiEvent

    /**
     * Событие установки позиции "земли" (нижней границы игрового поля).
     * 
     * @property groundPx Позиция земли в пикселях от верха экрана.
     *                    Используется для корректного отображения и расчета физики падения шариков.
     */
    data class SetGround(
        val groundPx: Float,
    ) : GameUiEvent
}

/**
 * ViewModel игрового экрана, управляющая состоянием и логикой игры.
 * 
 * Отвечает за:
 * - Управление игровым циклом
 * - Обработку пользовательского ввода
 * - Обновление игрового состояния
 * - Обработку игровой логики
 * - Взаимодействие с доменным слоем
 * 
 * @property handle [SavedStateHandle] для сохранения и восстановления состояния при повороте экрана.
 * @property spawnUC [SpawnBallUseCase] UseCase для создания новых шариков.
 * @property tickUC [TickPhysicsUseCase] UseCase для обновления физики игры.
 * @property scoreUC [UpdateScoreOnHitUseCase] UseCase для обновления счета при попадании.
 * @property dispatchers [DispatchersProvider] Провайдер корутин-диспетчеров.
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
            // Ключи для сохранения состояния в SavedStateHandle
            const val KEY_STATE = "state"          // Текущее состояние игры
            const val KEY_NEXT_SPAWN = "nextSpawnAt" // Время следующего появления шарика
            const val KEY_ARG_DIFFICULTY = "difficulty" // Уровень сложности
        }

        /**
         * Редьюсер, управляющий обновлением состояния игры на основе действий.
         * Может быть безопасно пересоздан при изменении позиции земли.
         * 
         * @see GameReducer
         */
        private var reducer =
            GameReducer(
                tick = tickUC,
                spawn = spawnUC,
                score = scoreUC,
                groundY = 1000f, // Начальное значение, будет обновлено при инициализации вью
            )

        /**
         * Текущее состояние игры с геттером и сеттером, сохраняющим состояние.
         * 
         * При первом обращении инициализируется в следующем порядке:
         * 1. Пытается восстановить из SavedStateHandle
         * 2. Если не найдено, создает новое на основе сложности из аргументов
         * 3. Если аргументы не переданы, использует NORMAL сложность по умолчанию
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

        /**
         * Планировщик появления новых шариков.
         * Отвечает за определение момента появления нового шарика на основе времени.
         */
        private val scheduler =
            SpawnScheduler(handle.get<Long>(KEY_NEXT_SPAWN) ?: SystemClock.uptimeMillis())

        /**
         * Поток состояния UI, доступный только для чтения.
         * Используется для обновления UI при изменении состояния игры.
         */
        private val _ui = MutableStateFlow(GameUiState(state))
        val ui: StateFlow<GameUiState> = _ui.asStateFlow()

        /**
         * Канал для отправки одноразовых эффектов (навигация, звуки, вибрация и т.д.).
         * Эффекты обрабатываются в UI-слое и не сохраняются при повороте экрана.
         */
        private val _effects = Channel<GameEffect>(Channel.BUFFERED)
        val effects: Flow<GameEffect> = _effects.receiveAsFlow()

        /**
         * Игровой цикл, работающий с частотой ~60 FPS (1000ms / 60 ≈ 16ms на кадр).
         * Отвечает за плавное обновление физики и анимаций.
         */
        private val loop = GameLoop(frameMs = 16L)

        init {
            // Запускаем игровой цикл в корутине
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
         * @param e Событие от UI, которое нужно обработать.
         */
        fun onEvent(e: GameUiEvent) {
            when (e) {
                // Обработка нажатия на кнопку паузы/продолжения
                GameUiEvent.PauseToggle -> 
                    onAction(GameAction.PauseToggle)
                
                // Обработка нажатия на шарик
                GameUiEvent.OnBallTap -> 
                    onAction(GameAction.Tap)
                
                // Обработка изменения позиции земли (при изменении размера экрана)
                is GameUiEvent.SetGround -> 
                    // Пересоздаем редьюсер с новой позицией земли
                    // Это необходимо, так как позиция земли влияет на расчеты физики
                    reducer =
                        GameReducer(
                            tick = tickUC,
                            spawn = spawnUC,
                            score = scoreUC,
                            groundY = e.groundPx, // Новая позиция земли в пикселях
                        )
            }
        }

        /**
         * Обработчик игровых действий, обновляющий состояние игры.
         * 
         * @param action Действие, которое нужно обработать.
         */
        private fun onAction(action: GameAction) {
            // Получаем новое состояние и эффект из редьюсера
            val (newState, effect) = reducer.reduce(state, action)
            
            // Если состояние изменилось, обновляем UI
            if (newState != state) {
                // Сохраняем текущее состояние в SavedStateHandle
                // для восстановления при повороте экрана
                state = newState
                
                // Сохраняем время следующего появления шарика
                handle[KEY_NEXT_SPAWN] = scheduler.snapshot()
                
                // Обновляем UI с новым состоянием
                _ui.value =
                    _ui.value.copy(
                        state = newState,
                        // Обновляем время только для тиков, для остальных действий оставляем прежнее
                        timeMs = (action as? GameAction.Tick)?.nowMs ?: _ui.value.timeMs,
                    )
            }
            
            // Если есть эффект (например, звук, вибрация, навигация),
            // отправляем его в канал для обработки в UI-слое
            if (effect != null) {
                viewModelScope.launch { 
                    _effects.send(effect) 
                }
            }
        }
    }
