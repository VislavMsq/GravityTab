package com.FDGEntertain.domain.usecase

import com.FDGEntertain.domain.model.Difficulty
import com.FDGEntertain.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SettingsState(
    val difficulty: Difficulty,
    val sound: Boolean,
)

class GetSettingsUseCase(
    private val repo: SettingsRepository,
) {
    operator fun invoke(): Flow<SettingsState> = repo.difficultyAndSound.map { (d, s) -> SettingsState(d, s) }
}
