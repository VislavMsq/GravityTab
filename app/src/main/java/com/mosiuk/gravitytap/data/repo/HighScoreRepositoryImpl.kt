package com.mosiuk.gravitytap.data.repo

import com.mosiuk.gravitytap.data.db.ScoreDao
import com.mosiuk.gravitytap.domain.model.ScoreEntry
import com.mosiuk.gravitytap.domain.repo.HighScoreRepository
import javax.inject.Inject

class HighScoreRepositoryImpl
    @Inject
    constructor(
        private val dao: ScoreDao,
    ) : HighScoreRepository {
        override suspend fun save(entry: ScoreEntry) = dao.insert(entry)

        override fun top(limit: Int) = dao.top(limit)
    }
