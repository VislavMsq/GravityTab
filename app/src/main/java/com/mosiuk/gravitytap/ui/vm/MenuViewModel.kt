package com.mosiuk.gravitytap.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.usecase.GetSettingsUseCase
import com.mosiuk.gravitytap.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Пакет содержит ViewModel'и экранов приложения.
 * 
 * ViewModel'и отвечают за хранение и обработку данных, связанных с UI,
 * а также за бизнес-логику приложения.
 */

/**
 * Класс, представляющий состояние пользовательского интерфейса экрана меню.
 * 
 * @property difficulty Текущий выбранный уровень сложности. Влияет на параметры игры,
 *                     такие как скорость появления шариков и количество жизней.
 * @property sound Флаг, указывающий, включен ли звук в приложении.
 *                 При значении `true` звуковые эффекты воспроизводятся, при `false` - отключены.
 * @property isBusy Флаг, указывающий на выполнение фоновой операции (например, загрузки данных).
 *                  Используется для отображения индикатора загрузки в UI.
 */
data class MenuUiState(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val isBusy: Boolean = false,
)

/**
 * Запечатанный интерфейс, представляющий события пользовательского интерфейса экрана меню.
 * 
 * События генерируются в ответ на действия пользователя и обрабатываются в [MenuViewModel].
 * Каждое событие соответствует определенному пользовательскому действию, такому как
 * выбор уровня сложности, переключение звука или начало игры.
 */
sealed interface MenuEvent {
    /**
     * Событие выбора уровня сложности в меню настроек.
     *
     * @property value Выбранный уровень сложности из доступных вариантов:
     *                 - [Difficulty.EASY] - легкий уровень для новичков
     *                 - [Difficulty.NORMAL] - стандартный уровень сложности
     *                 - [Difficulty.HARD] - сложный уровень для опытных игроков
     */
    data class SelectDifficulty(
        val value: Difficulty,
    ) : MenuEvent

    /**
     * Событие переключения звука в настройках игры.
     *
     * @property value Новое состояние звука:
     *                 - `true` - звук включен
     *                 - `false` - звук отключен
     */
    data class ToggleSound(
        val value: Boolean,
    ) : MenuEvent

    /**
     * Событие нажатия на кнопку начала игры.
     * 
     * Инициирует переход на игровой экран с текущими настройками сложности.
     * Перед переходом проверяет корректность состояния приложения.
     */
    data object StartClicked : MenuEvent
}

/**
 * Запечатанный интерфейс, представляющий эффекты, которые могут быть вызваны из [MenuViewModel].
 * 
 * Эффекты используются для одноразовых действий, таких как навигация между экранами,
 * отображение уведомлений или выполнение других побочных эффектов, которые не являются
 * частью состояния UI, но должны быть обработаны в UI-слое.
 */
sealed interface MenuEffect {
    /**
     * Эффект навигации с экрана меню на игровой экран.
     * 
     * @property difficulty Уровень сложности, с которым будет запущена новая игра.
     *                     Передается в игровой экран для инициализации игрового процесса
     *                     с выбранными параметрами.
     */
    data class NavigateToGame(
        val difficulty: Difficulty,
    ) : MenuEffect
}

/**
 * ViewModel для экрана главного меню приложения.
 * 
 * Отвечает за:
 * - Управление состоянием UI главного меню
 * - Обработку пользовательского ввода (выбор сложности, настройка звука)
 * - Навигацию на игровой экран
 * - Сохранение и восстановление состояния при повороте экрана
 *
 * @property getSettings UseCase для получения текущих настроек приложения из хранилища.
 * @property updateSettings UseCase для обновления настроек приложения в хранилище.
 * @property dispatchers Провайдер корутин-диспетчеров для выполнения операций ввода-вывода.
 * @property handle [SavedStateHandle] для сохранения и восстановления состояния при повороте экрана.
 */
@HiltViewModel
class MenuViewModel
    @Inject
    constructor(
        private val getSettings: GetSettingsUseCase,
        private val updateSettings: UpdateSettingsUseCase,
        private val dispatchers: DispatchersProvider,
        private val handle: SavedStateHandle,
    ) : ViewModel() {
        /**
         * Приватный изменяемый поток состояния UI.
         * 
         * Инициализируется значениями из [SavedStateHandle] (при наличии) или значениями по умолчанию.
         * Содержит актуальное состояние UI экрана меню.
         */
        private val _ui =
            MutableStateFlow(
                MenuUiState(
                    difficulty = handle.get<Difficulty>("menu_difficulty") ?: Difficulty.NORMAL,
                    sound = handle.get<Boolean>("menu_sound") ?: true,
                ),
            )
        
        /**
         * Публичный неизменяемый поток состояния UI.
         * 
         * Используется UI-слоем для подписки на изменения состояния меню.
         * Предоставляет доступ только для чтения к текущему состоянию UI.
         */
        val ui: StateFlow<MenuUiState> = _ui.asStateFlow()

        /**
         * Приватный канал для отправки одноразовых эффектов.
         * 
         * Используется для передачи событий, которые должны быть обработаны один раз,
         * таких как навигация между экранами или показ уведомлений.
         */
        private val _effects = Channel<MenuEffect>(capacity = Channel.BUFFERED)
        
        /**
         * Публичный поток для подписки на эффекты.
         * 
         * UI-слой должен подписаться на этот поток и обрабатывать приходящие эффекты,
         * такие как навигация или отображение уведомлений.
         */
        val effects: Flow<MenuEffect> = _effects.receiveAsFlow()

        init {
            // Запускаем сборщик настроек при инициализации ViewModel
            viewModelScope.launch(dispatchers.io) {
                // Подписываемся на поток настроек из DataStore
                getSettings().collect { settings ->
                    // Обновляем состояние UI с новыми настройками
                    _ui.update { currentState ->
                        currentState.copy(
                            difficulty = settings.difficulty,
                            sound = settings.sound
                        )
                    }
                    
                    // Сохраняем текущие настройки в SavedStateHandle
                    // для восстановления при повороте экрана или пересоздании активити
                    handle["menu_difficulty"] = settings.difficulty
                    handle["menu_sound"] = settings.sound
                }
            }
        }

        /**
         * Обработчик событий от пользовательского интерфейса.
         * 
         * Принимает события от UI, обрабатывает их и обновляет состояние приложения.
         * В зависимости от типа события может:
         * - Обновлять настройки приложения
         * - Менять состояние UI
         * - Инициировать навигацию на другие экраны
         *
         * @param e Событие, которое нужно обработать. Может быть одним из:
         *          - [MenuEvent.SelectDifficulty] - изменение уровня сложности
         *          - [MenuEvent.ToggleSound] - переключение звука
         *          - [MenuEvent.StartClicked] - начало новой игры
         */
        fun onEvent(e: MenuEvent) {
            when (e) {
                // Обработка изменения уровня сложности
                is MenuEvent.SelectDifficulty -> {
                    // Обновляем настройки в фоновом потоке ввода-вывода
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateDifficulty(e.value)
                        // UI обновится автоматически через Flow в init
                    }
                }

                // Обработка переключения звука
                is MenuEvent.ToggleSound -> {
                    // Обновляем настройки звука в фоновом потоке ввода-вывода
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateSound(e.value)
                        // UI обновится автоматически через Flow в init
                    }
                }

                // Обработка нажатия на кнопку начала игры
                MenuEvent.StartClicked -> {
                    // Получаем выбранный уровень сложности
                    val selectedDifficulty = _ui.value.difficulty
                    
                    // Запускаем корутину для отправки эффекта навигации
                    viewModelScope.launch {
                        // Отправляем эффект навигации с выбранным уровнем сложности
                        // UI слой должен обработать этот эффект и перейти на игровой экран
                        _effects.send(MenuEffect.NavigateToGame(selectedDifficulty))
                    }
                }
            }
        }
    }
