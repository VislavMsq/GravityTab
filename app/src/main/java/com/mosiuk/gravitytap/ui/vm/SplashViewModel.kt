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

/**
 * Состояние UI экрана загрузки (сплэш-экрана).
 *
 * @property progress Прогресс загрузки в диапазоне от 0.0 до 1.0
 * @property started Флаг, указывающий на начало процесса загрузки
 */
data class SplashUiState(
    val progress: Float = 0f,
    val started: Boolean = false,
)

/**
 * Эффекты, которые могут быть вызваны из ViewModel сплэш-экрана.
 */
sealed interface SplashEffect {
    /** Эффект навигации на главное меню */
    data object NavigateToMenu : SplashEffect
}

/**
 * ViewModel для сплэш-экрана приложения.
 * Управляет процессом загрузки и навигацией на главный экран.
 *
 * @property handle SavedStateHandle для сохранения состояния при повороте экрана
 * @property dispatchers Провайдер корутин-диспетчеров
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        // Приватный поток состояния UI с восстановлением из SavedStateHandle
        private val _ui =
            MutableStateFlow(
                SplashUiState(
                    progress = handle["progress"] ?: 0f,
                    started = handle["started"] ?: false,
                ),
            )
        
        /** Публичный неизменяемый поток состояния UI */
        val ui = _ui.asStateFlow()

        // Приватный канал для отправки одноразовых эффектов (навигация)
        private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
        
        /** Публичный поток для подписки на эффекты */
        val effects: Flow<SplashEffect> = _effects.receiveAsFlow()

        init {
            // Проверяем, был ли уже показан сплэш (например, после поворота экрана)
            if (handle.get<Boolean>("done") == true) {
                // Если да, сразу переходим в главное меню
                viewModelScope.launch { _effects.send(SplashEffect.NavigateToMenu) }
            } else if (!_ui.value.started) {
                // Если нет, запускаем процесс загрузки
                startLoading()
            }
        }

        /**
         * Запускает процесс имитации загрузки с анимацией прогресса.
         * Обновляет прогресс с заданным интервалом и по завершении инициирует навигацию.
         */
        private fun startLoading() {
            // Помечаем процесс загрузки как начатый
            _ui.update { it.copy(started = true) }
            handle["started"] = true
            
            viewModelScope.launch(dispatchers.main) {
                // Имитация загрузки с обновлением прогресса
                // Общее время анимации ~1200ms (24ms * 50 шагов)
                var progress = _ui.value.progress
                while (progress < 1f) {
                    // Увеличиваем прогресс с шагом 0.05 (5%), но не более 1.0
                    progress = (progress + 0.05f).coerceAtMost(1f)
                    
                    // Обновляем состояние и сохраняем прогресс
                    _ui.update { it.copy(progress = progress) }
                    handle["progress"] = progress
                    
                    // Пауза между обновлениями (24ms ≈ 40 FPS)
                    delay(24L)
                }
                
                // Помечаем загрузку как завершенную и переходим в меню
                handle["done"] = true
                _effects.send(SplashEffect.NavigateToMenu)
            }
        }
    }
