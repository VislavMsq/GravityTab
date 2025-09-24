package com.FDGEntertain.domain.game

import com.FDGEntertain.domain.model.Difficulty


sealed interface GameEffect {
    data class GameOver(
        val score: Int,
        val difficulty: Difficulty,
        val maxCombo: Int,
    ) : GameEffect
}
