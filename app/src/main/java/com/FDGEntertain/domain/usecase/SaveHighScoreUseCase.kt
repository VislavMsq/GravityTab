package com.FDGEntertain.domain.usecase

import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.domain.model.ScoreEntry
import com.FDGEntertain.domain.repo.HighScoreRepository
import javax.inject.Inject

class SaveHighScoreUseCase @Inject constructor(
    private val repo: HighScoreRepository,
) {
    suspend operator fun invoke(
        score: Int,
        difficulty: Difficulty,
        maxCombo: Int,
        date: Long = System.currentTimeMillis(),
    ) {
        repo.save(
            ScoreEntry(
                date = date,
                score = score,
                difficulty = difficulty.name,
                maxCombo = maxCombo
            )
        )
    }
}
