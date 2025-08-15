package com.mosiuk.gravitytap.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.usecase.GetHighScoresUseCase
import com.mosiuk.gravitytap.domain.usecase.SaveHighScoreUseCase
import com.mosiuk.gravitytap.ui.result.ResultUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана результатов игры, управляющий отображением результатов
 * и таблицы рекордов.
 *
 * @property handle SavedStateHandle для сохранения состояния при повороте экрана
 * @property save UseCase для сохранения результатов игры
 * @property getTop UseCase для получения списка лучших результатов
 * @property dispatchers Провайдер корутин-диспетчеров
 */
@HiltViewModel
class ResultViewModel
    @Inject
    constructor(
        private val handle: SavedStateHandle,
        private val save: SaveHighScoreUseCase,
        getTop: GetHighScoresUseCase,
        private val dispatchers: DispatchersProvider,
    ) : ViewModel() {
        private companion object {
            // Ключ для отслеживания сохранения результата
            const val KEY_SAVED = "result_saved"
        }

        // Текущий счет из аргументов навигации
        private val score: Int = handle.get<Int>("score") ?: 0
        
        // Уровень сложности из аргументов навигации с обработкой различных форматов
        private val difficulty: Difficulty =
            run {
                // Получаем и нормализуем строковое значение сложности
                val raw =
                    handle
                        .get<String>("difficulty")
                        ?.trim()
                        ?.removePrefix("{")  // На случай, если пришло значение в фигурных скобках
                        ?.removeSuffix("}")
                        ?.uppercase()

                // Ищем соответствующий уровень сложности или используем NORMAL по умолчанию
                Difficulty.entries.firstOrNull { it.name == raw } ?: Difficulty.NORMAL
            }
            
        // Максимальное комбо из аргументов навигации
        private val maxCombo: Int = handle.get<Int>("maxCombo") ?: 0

        /**
         * UI-модель с результатами текущей игры.
         */
        val ui: ResultUi = ResultUi(score, difficulty, maxCombo)

        /**
         * Поток с топом результатов.
         * Кэшируется на 5 секунд после отписки последнего подписчика.
         */
        val top: StateFlow<List<ScoreEntry>> =
            getTop().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

        init {
            // Сохраняем результат, если он еще не был сохранен
            if (handle.get<Boolean>(KEY_SAVED) != true) {
                viewModelScope.launch(dispatchers.io) {
                    // Сохраняем результат в репозиторий
                    save(score, difficulty, maxCombo)
                    // Помечаем, что результат сохранен, чтобы избежать дублирования
                    handle.set(KEY_SAVED, true)
                }
            }
        }
    }
