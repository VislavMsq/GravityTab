package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.repo.HighScoreRepository

class GetHighScoresUseCase(private val repo: HighScoreRepository) {
    operator fun invoke(limit: Int = 20) = repo.top(limit)
}