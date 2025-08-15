package com.mosiuk.gravitytap.ui.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val progress: Float = 0f, // 0f..1f
    val started: Boolean = false,
)

sealed interface SplashEffect {
    data object NavigateToMenu : SplashEffect
}

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        private val _ui =
            MutableStateFlow(
                SplashUiState(
                    progress = handle["progress"] ?: 0f,
                    started = handle["started"] ?: false,
                ),
            )
        val ui = _ui.asStateFlow()

        private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
        val effects: Flow<SplashEffect> = _effects.receiveAsFlow()

        init {
            // Если уже успели пройти сплэш (например, ротация) — сразу навигируем
            if (handle.get<Boolean>("done") == true) {
                viewModelScope.launch { _effects.send(SplashEffect.NavigateToMenu) }
            } else if (!_ui.value.started) {
                startLoading()
            }
        }

        private fun startLoading() {
            _ui.update { it.copy(started = true) }
            handle["started"] = true
            viewModelScope.launch(dispatchers.main) {
                // Имитируем загрузку ~1200ms с шагом 24ms
                var p = _ui.value.progress
                while (p < 1f) {
                    p = (p + 0.05f).coerceAtMost(1f)
                    _ui.update { it.copy(progress = p) }
                    handle["progress"] = p
                    delay(24L)
                }
                handle["done"] = true
                _effects.send(SplashEffect.NavigateToMenu)
            }
        }
    }
