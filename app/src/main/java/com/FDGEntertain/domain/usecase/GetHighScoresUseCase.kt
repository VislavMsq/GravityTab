package com.FDGEntertain.domain.usecase

import com.FDGEntertain.domain.model.ScoreEntry
import com.FDGEntertain.domain.repo.HighScoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHighScoresUseCase @Inject constructor(
    private val repo: HighScoreRepository,
) {
    operator fun invoke(limit: Int = 20): Flow<List<ScoreEntry>> = repo.top(limit)
}
