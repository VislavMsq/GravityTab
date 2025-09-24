package com.FDGEntertain.core.di

import com.FDGEntertain.data.repo.HighScoreRepositoryImpl
import com.FDGEntertain.data.repo.SettingsRepositoryImpl
import com.FDGEntertain.domain.repo.HighScoreRepository
import com.FDGEntertain.domain.repo.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds @Singleton
    abstract fun bindScores(impl: HighScoreRepositoryImpl): HighScoreRepository

    @Binds @Singleton
    abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
}
