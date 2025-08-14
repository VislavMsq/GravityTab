package com.mosiuk.gravitytap.data.repo

import com.mosiuk.gravitytap.data.datastore.SettingsDataStore
import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.repo.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val ds: SettingsDataStore,
) : SettingsRepository {
    override val difficultyAndSound = ds.settings
    override suspend fun updateDifficulty(d: Difficulty) = ds.updateDifficulty(d)
    override suspend fun updateSound(on: Boolean) = ds.updateSound(on)

}