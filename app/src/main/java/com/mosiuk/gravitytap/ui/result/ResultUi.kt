package com.mosiuk.gravitytap.ui.result

import com.mosiuk.gravitytap.domain.model.Difficulty

/**
 * Модель данных для отображения результатов игры.
 *
 * @property score Количество набранных очков
 * @property difficulty Уровень сложности, на котором был достигнут результат
 * @property maxCombo Максимальное комбо, достигнутое за игру
 */
data class ResultUi(
    val score: Int,
    val difficulty: Difficulty,
    val maxCombo: Int,
)
