package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.repo.SettingsRepository

class UpdateSettingsUseCase(
    private val repo: SettingsRepository,
) {
    suspend fun updateDifficulty(d: Difficulty) = repo.updateDifficulty(d)

    suspend fun updateSound(on: Boolean) = repo.updateSound(on)
}
