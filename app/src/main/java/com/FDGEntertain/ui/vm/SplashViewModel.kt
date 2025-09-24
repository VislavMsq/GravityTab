package com.mosiuk.FDGEntertain.ui.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FDGEntertain.core.util.DispatchersProvider
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
 * Пакет содержит ViewModel'и экранов приложения.
 * 
 * ViewModel'и отвечают за хранение и обработку данных, связанных с UI,
 * а также за бизнес-логику приложения.
 */

/**
 * Класс, представляющий состояние пользовательского интерфейса экрана загрузки.
 * 
 * @property progress Текущий прогресс загрузки в диапазоне от 0.0 до 1.0, где:
 *                   - 0.0 - загрузка не начата
 *                   - 1.0 - загрузка завершена
 *                   Промежуточные значения отображают процент выполнения.
 * @property started Флаг, указывающий на начало процесса загрузки.
 *                  Используется для предотвращения повторного запуска загрузки
 *                  при повороте экрана или пересоздании активити.
 */
data class SplashUiState(
    val progress: Float = 0f,
    val started: Boolean = false,
)

/**
 * Запечатанный интерфейс, представляющий эффекты, которые могут быть вызваны из [SplashViewModel].
 * 
 * Эффекты используются для одноразовых действий, таких как навигация между экранами.
 * Каждый эффект представляет собой событие, которое должно быть обработано UI-слоем.
 */
sealed interface SplashEffect {
    /** 
     * Эффект навигации на главный экран приложения.
     * 
     * Отправляется после завершения анимации загрузки и готовности к переходу.
     * UI-слой должен обработать этот эффект и выполнить навигацию на главный экран.
     */
    data object NavigateToMenu : SplashEffect
}

/**
 * ViewModel для экрана загрузки (сплэш-экрана) приложения.
 * 
 * Отвечает за:
 * - Управление анимацией загрузки с плавным заполнением прогресс-бара
 * - Восстановление состояния при повороте экрана
 * - Навигацию на главный экран по завершении загрузки
 * - Обработку конфигурационных изменений без потери прогресса
 *
 * @property handle [SavedStateHandle] для сохранения и восстановления состояния при повороте экрана.
 * @property dispatchers [DispatchersProvider] для работы с корутинами в соответствующих потоках.
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
    /**
     * Приватный изменяемый поток состояния UI.
     *
     * Инициализируется значениями из [SavedStateHandle] (при наличии) или значениями по умолчанию.
     * Содержит актуальное состояние UI экрана загрузки.
     */
    private val _ui =
        MutableStateFlow(
            SplashUiState(
                progress = handle["progress"] ?: 0f,
                started = handle["started"] ?: false,
            ),
        )

    /**
     * Публичный неизменяемый поток состояния UI.
     *
     * Используется UI-слоем для подписки на изменения состояния загрузки.
     * Предоставляет доступ только для чтения к текущему состоянию UI.
     */
    val ui = _ui.asStateFlow()

    /**
     * Приватный канал для отправки одноразовых эффектов.
     *
     * Используется для передачи событий, которые должны быть обработаны один раз,
     * таких как навигация между экранами.
     */
    private val _effects = Channel<SplashEffect>(capacity = Channel.BUFFERED)

    /**
     * Публичный поток для подписки на эффекты.
     *
     * UI-слой должен подписаться на этот поток и обрабатывать приходящие эффекты,
     * такие как навигация на главный экран.
     */
    val effects: Flow<SplashEffect> = _effects.receiveAsFlow()

    init {
        // Проверяем, была ли уже завершена загрузка (например, после поворота экрана)
        if (handle.get<Boolean>("done") == true) {
            // Если загрузка уже завершена, сразу переходим в главное меню
            viewModelScope.launch {
                _effects.send(SplashEffect.NavigateToMenu)
            }
        } else if (!_ui.value.started) {
            // Если загрузка еще не начата, запускаем процесс загрузки
            startLoading()
        } else {
            // Если загрузка была прервана (например, при повороте экрана),
            // продолжаем с последнего сохраненного прогресса
            startLoading()
        }
    }

    /**
     * Запускает процесс имитации загрузки с анимацией прогресса.
     *
     * Метод выполняет следующие действия:
     * 1. Помечает процесс загрузки как начатый
     * 2. Запускает корутину для анимации прогресс-бара
     * 3. Обновляет состояние UI с текущим прогрессом
     * 4. Сохраняет прогресс в SavedStateHandle для восстановления
     * 5. По завершении инициирует переход на главный экран
     *
     * @see SplashUiState.progress
     * @see SplashEffect.NavigateToMenu
     */
    private fun startLoading() {
        _ui.update { it.copy(started = true) }
        handle["started"] = true

        viewModelScope.launch(dispatchers.main) {
            var progress = _ui.value.progress

            // Настраиваемая длительность и плавность
            val totalDurationMs = 3000L       // << выбери 2000–4000L по вкусу
            val totalSteps = 60               // ~60 кадров
            val delayPerStep = (totalDurationMs / totalSteps).coerceAtLeast(16L)

            // Сколько шагов осталось исходя из текущего прогресса
            val remaining = (1f - progress).coerceIn(0f, 1f)
            val stepsLeft = (totalSteps * remaining).coerceAtLeast(1f).toInt()
            val delta = if (stepsLeft > 0) remaining / stepsLeft else remaining

            repeat(stepsLeft) {
                progress = (progress + delta).coerceAtMost(1f)
                _ui.update { it.copy(progress = progress) }
                handle["progress"] = progress
                delay(delayPerStep)
            }

            handle["done"] = true
            _effects.send(SplashEffect.NavigateToMenu)
        }
    }
}

