package com.mosiuk.gravitytap.core.di

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
}