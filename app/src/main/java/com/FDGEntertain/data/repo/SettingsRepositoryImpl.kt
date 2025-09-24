package com.FDGEntertain.data.repo

import com.FDGEntertain.data.datastore.SettingsDataStore
import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl
    @Inject
    constructor(
        private val ds: SettingsDataStore,
    ) : SettingsRepository {
        override val difficultyAndSound: Flow<Pair<Difficulty, Boolean>> = ds.settings

        override suspend fun updateDifficulty(d: Difficulty) = ds.updateDifficulty(d)

        override suspend fun updateSound(sound: Boolean) = ds.updateSound(sound)
    }
