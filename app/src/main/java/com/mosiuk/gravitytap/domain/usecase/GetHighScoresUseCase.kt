package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.repo.HighScoreRepository
import kotlinx.coroutines.flow.Flow

class GetHighScoresUseCase(
    private val repo: HighScoreRepository,
) {
    operator fun invoke(limit: Int = 20): Flow<List<ScoreEntry>> = repo.top(limit)
}
