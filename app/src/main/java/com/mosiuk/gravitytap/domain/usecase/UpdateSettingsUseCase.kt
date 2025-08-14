package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.Difficulty
import com.mosiuk.gravitytap.domain.repo.SettingsRepository

class UpdateSettingsUseCase(private val repo: SettingsRepository) {
    suspend fun difficulty(d: Difficulty) = repo.updateDifficulty(d)
    suspend fun sound(on: Boolean) = repo.updateSound(on)
}