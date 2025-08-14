package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.repo.HighScoreRepository

class SaveHighScoreUseCase(private val repo: HighScoreRepository) {
    suspend operator fun invoke(entry: ScoreEntry) = repo.save(entry)
}