package com.FDGEntertain.domain.repo

import com.FDGEntertain.domain.model.ScoreEntry
import kotlinx.coroutines.flow.Flow

interface HighScoreRepository {
    suspend fun save(entry: ScoreEntry)

    fun top(limit: Int = 20): Flow<List<ScoreEntry>>
}
