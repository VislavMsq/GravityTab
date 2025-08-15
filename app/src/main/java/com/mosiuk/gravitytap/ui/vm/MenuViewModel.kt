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

data class MenuUiState(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val isBusy: Boolean = false,
)

sealed interface MenuEvent {
    data class SelectDifficulty(
        val value: Difficulty,
    ) : MenuEvent

    data class ToggleSound(
        val value: Boolean,
    ) : MenuEvent

    data object StartClicked : MenuEvent
}

sealed interface MenuEffect {
    data class NavigateToGame(
        val difficulty: Difficulty,
    ) : MenuEffect
}

@HiltViewModel
class MenuViewModel
    @Inject
    constructor(
        private val getSettings: GetSettingsUseCase,
        private val updateSettings: UpdateSettingsUseCase,
        private val dispatchers: DispatchersProvider,
        private val handle: SavedStateHandle,
    ) : ViewModel() {
        private val _ui =
            MutableStateFlow(
                MenuUiState(
                    difficulty = handle.get<Difficulty>("menu_difficulty") ?: Difficulty.NORMAL,
                    sound = handle.get<Boolean>("menu_sound") ?: true,
                ),
            )
        val ui: StateFlow<MenuUiState> = _ui.asStateFlow()

        private val _effects = Channel<MenuEffect>(capacity = Channel.BUFFERED)
        val effects: Flow<MenuEffect> = _effects.receiveAsFlow()

        init {
            // Подписка на DataStore: поддерживаем состояние и SavedStateHandle в актуальном виде
            viewModelScope.launch(dispatchers.io) {
                getSettings().collect { st ->
                    _ui.update { it.copy(difficulty = st.difficulty, sound = st.sound) }
                    handle["menu_difficulty"] = st.difficulty
                    handle["menu_sound"] = st.sound
                }
            }
        }

        fun onEvent(e: MenuEvent) {
            when (e) {
                is MenuEvent.SelectDifficulty ->
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateDifficulty(e.value)
                    }

                is MenuEvent.ToggleSound ->
                    viewModelScope.launch(dispatchers.io) {
                        updateSettings.updateSound(e.value)
                    }

                MenuEvent.StartClicked -> {
                    val d = _ui.value.difficulty
                    viewModelScope.launch {
                        _effects.send(MenuEffect.NavigateToGame(d))
                    }
                }
            }
        }
    }
