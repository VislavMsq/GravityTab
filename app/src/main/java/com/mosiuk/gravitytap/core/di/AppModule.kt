package com.mosiuk.gravitytap.core.di

import com.mosiuk.gravitytap.core.util.DefaultDispatchers
import com.mosiuk.gravitytap.core.util.DispatchersProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatchers(): DispatchersProvider = DefaultDispatchers()
}