package com.mosiuk.gravitytap.core.di

import com.mosiuk.gravitytap.data.repo.HighScoreRepositoryImpl
import com.mosiuk.gravitytap.data.repo.SettingsRepositoryImpl
import com.mosiuk.gravitytap.domain.repo.HighScoreRepository
import com.mosiuk.gravitytap.domain.repo.SettingsRepository
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
