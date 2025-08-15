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
 * Состояние UI экрана меню.
 *
 * @property difficulty Текущий выбранный уровень сложности
 * @property sound Включен ли звук в игре
 * @property isBusy Флаг, указывающий на выполнение операции (например, загрузки)
 */
data class MenuUiState(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val isBusy: Boolean = false,
)

/**
 * События пользовательского интерфейса экрана меню.
 */
sealed interface MenuEvent {
    /**
     * Событие выбора уровня сложности.
     *
     * @property value Выбранный уровень сложности
     */
    data class SelectDifficulty(
        val value: Difficulty,
    ) : MenuEvent

    /**
     * Событие переключения звука.
     *
     * @property value Новое состояние звука (включен/выключен)
     */
    data class ToggleSound(
        val value: Boolean,
    ) : MenuEvent

    /** Событие нажатия на кнопку начала игры */
    data object StartClicked : MenuEvent
}

/**
 * Эффекты, которые могут быть вызваны из ViewModel меню.
 */
sealed interface MenuEffect {
    /**
     * Эффект навигации на игровой экран.
     *
     * @property difficulty Уровень сложности, с которым нужно начать игру
     */
    data class NavigateToGame(
        val difficulty: Difficulty,
    ) : MenuEffect
}

/**
 * ViewModel для экрана меню, управляющая состоянием и логикой главного меню.
 *
 * @property getSettings UseCase для получения настроек приложения
 * @property updateSettings UseCase для обновления настроек
 * @property dispatchers Провайдер корутин-диспетчеров
 * @property handle SavedStateHandle для сохранения состояния при повороте экрана
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
        // Приватный поток состояния UI с начальными значениями из SavedStateHandle
        private val _ui =
            MutableStateFlow(
                MenuUiState(
                    difficulty = handle.get<Difficulty>("menu_difficulty") ?: Difficulty.NORMAL,
                    sound = handle.get<Boolean>("menu_sound") ?: true,
                ),
            )
        
        /** Публичный неизменяемый поток состояния UI */
        val ui: StateFlow<MenuUiState> = _ui.asStateFlow()

        // Приватный канал для отправки одноразовых эффектов (навигация и т.д.)
        private val _effects = Channel<MenuEffect>(capacity = Channel.BUFFERED)
        
        /** Публичный поток для подписки на эффекты */
        val effects: Flow<MenuEffect> = _effects.receiveAsFlow()

        init {
            // Подписка на изменения настроек из DataStore
            viewModelScope.launch(dispatchers.io) {
                getSettings().collect { settings ->
                    // Обновляем UI и сохраняем состояние при изменении настроек
                    _ui.update { it.copy(difficulty = settings.difficulty, sound = settings.sound) }
                    
                    // Сохраняем текущие настройки в SavedStateHandle для восстановления при повороте
                    handle["menu_difficulty"] = settings.difficulty
                    handle["menu_sound"] = settings.sound
                }
            }
        }

        /**
         * Обработчик событий от пользовательского интерфейса.
         *
         * @param e Событие от UI
         */
        fun onEvent(e: MenuEvent) {
            when (e) {
                // Обработка изменения уровня сложности
                is MenuEvent.SelectDifficulty ->
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateDifficulty(e.value)
                    }

                // Обработка переключения звука
                is MenuEvent.ToggleSound ->
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateSound(e.value)
                    }

                // Обработка нажатия на кнопку начала игры
                MenuEvent.StartClicked -> {
                    val selectedDifficulty = _ui.value.difficulty
                    viewModelScope.launch {
                        // Отправляем эффект навигации с выбранным уровнем сложности
                        _effects.send(MenuEffect.NavigateToGame(selectedDifficulty))
                    }
                }
            }
        }
    }
