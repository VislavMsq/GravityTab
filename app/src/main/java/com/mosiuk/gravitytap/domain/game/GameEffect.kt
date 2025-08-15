package com.mosiuk.gravitytap.domain.game

import com.mosiuk.gravitytap.domain.model.Difficulty

sealed interface GameEffect {
    data class GameOver(
        val score: Int,
        val difficulty: Difficulty,
        val maxCombo: Int,
    ) : GameEffect
}
