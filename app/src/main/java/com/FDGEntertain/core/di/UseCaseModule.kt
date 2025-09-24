package com.FDGEntertain.core.di

import com.FDGEntertain.domain.repo.HighScoreRepository
import com.FDGEntertain.domain.repo.SettingsRepository
import com.FDGEntertain.domain.usecase.GetHighScoresUseCase
import com.FDGEntertain.domain.usecase.GetSettingsUseCase
import com.FDGEntertain.domain.usecase.SaveHighScoreUseCase
import com.FDGEntertain.domain.usecase.SpawnBallUseCase
import com.FDGEntertain.domain.usecase.TickPhysicsUseCase
import com.FDGEntertain.domain.usecase.UpdateScoreOnHitUseCase
import com.FDGEntertain.domain.usecase.UpdateSettingsUseCase
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
