package com.FDGEntertain.core.di

import com.FDGEntertain.core.util.DefaultDispatchers
import com.FDGEntertain.core.util.DispatchersProvider
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
