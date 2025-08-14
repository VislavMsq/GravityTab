package com.mosiuk.gravitytap.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @Binds @Singleton
    abstract fun bindScores(impl: ScoresRepositoryImpl): ScoresRepository

    @Binds @Singleton
    abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
}