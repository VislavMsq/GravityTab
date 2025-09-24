package com.FDGEntertain.data.repo

import com.FDGEntertain.data.db.ScoreDao
import com.FDGEntertain.domain.model.ScoreEntry
import com.FDGEntertain.domain.repo.HighScoreRepository
import javax.inject.Inject

class HighScoreRepositoryImpl
    @Inject
    constructor(
        private val dao: ScoreDao,
    ) : HighScoreRepository {
        override suspend fun save(entry: ScoreEntry) = dao.insert(entry)

        override fun top(limit: Int) = dao.top(limit)
    }
