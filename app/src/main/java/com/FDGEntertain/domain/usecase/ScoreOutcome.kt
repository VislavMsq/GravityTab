package com.FDGEntertain.domain.usecase

data class ScoreOutcome(
    val scoreDelta: Int,
    val newCombo: Int,
    val maxCombo: Int,
)

class UpdateScoreOnHitUseCase {
    fun onHit(
        currentScore: Int,
        combo: Int,
        maxCombo: Int,
    ): ScoreOutcome {
        val newCombo = combo + 1
        val add = 10 * newCombo
        return ScoreOutcome(currentScore + add, newCombo, maxOf(maxCombo, newCombo))
    }

    fun onMiss(
        currentScore: Int,
        combo: Int,
        maxCombo: Int,
    ): ScoreOutcome = ScoreOutcome(currentScore, 0, maxCombo)
}
