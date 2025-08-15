package com.mosiuk.gravitytap.core.di

import com.mosiuk.gravitytap.domain.repo.HighScoreRepository
import com.mosiuk.gravitytap.domain.repo.SettingsRepository
import com.mosiuk.gravitytap.domain.usecase.GetHighScoresUseCase
import com.mosiuk.gravitytap.domain.usecase.GetSettingsUseCase
import com.mosiuk.gravitytap.domain.usecase.SaveHighScoreUseCase
import com.mosiuk.gravitytap.domain.usecase.SpawnBallUseCase
import com.mosiuk.gravitytap.domain.usecase.TickPhysicsUseCase
import com.mosiuk.gravitytap.domain.usecase.UpdateScoreOnHitUseCase
import com.mosiuk.gravitytap.domain.usecase.UpdateSettingsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideSpawn() = SpawnBallUseCase()

    @Provides
    fun provideTick() = TickPhysicsUseCase()

    @Provides
    fun provideUpdateScore() = UpdateScoreOnHitUseCase()

    @Provides
    fun provideGetSettings(repo: SettingsRepository) = GetSettingsUseCase(repo)

    @Provides
    fun provideUpdateSettings(repo: SettingsRepository) = UpdateSettingsUseCase(repo)

    @Provides
    fun provideGetHighScores(repo: HighScoreRepository) = GetHighScoresUseCase(repo)

    @Provides
    fun provideSaveHighScore(repo: HighScoreRepository) = SaveHighScoreUseCase(repo)
}
