package com.FDGEntertain.domain.usecase

import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.domain.repo.SettingsRepository


class UpdateSettingsUseCase(
    private val repo: SettingsRepository,
) {
    suspend fun updateDifficulty(d: Difficulty) = repo.updateDifficulty(d)

    suspend fun updateSound(on: Boolean) = repo.updateSound(on)
}
