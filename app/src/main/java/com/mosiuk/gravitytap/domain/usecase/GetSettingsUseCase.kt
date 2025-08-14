package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.repo.SettingsRepository

class GetSettingsUseCase(private val repo: SettingsRepository) {
    operator fun invoke() = repo.difficultyAndSound
}