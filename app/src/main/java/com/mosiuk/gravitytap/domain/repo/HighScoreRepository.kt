package com.mosiuk.gravitytap.domain.repo

import com.mosiuk.gravitytap.domain.model.ScoreEntry
import kotlinx.coroutines.flow.Flow

interface HighScoreRepository {
    suspend fun save(entry: ScoreEntry)
    fun top(limit: Int = 20): Flow<List<ScoreEntry>>
}