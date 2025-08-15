package com.mosiuk.gravitytap.ui.result

import com.mosiuk.gravitytap.domain.model.Difficulty

data class ResultUi(
    val score: Int,
    val difficulty: Difficulty,
    val maxCombo: Int,
)
