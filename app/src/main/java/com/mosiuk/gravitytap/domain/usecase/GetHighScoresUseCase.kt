package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.repo.HighScoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoresUseCase @Inject constructor(
    private val repo: HighScoreRepository,
) {
    operator fun invoke(limit: Int = 20): Flow<List<ScoreEntry>> = repo.top(limit)
}
