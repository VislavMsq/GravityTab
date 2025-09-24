// core/di/DataModule.kt
package com.FDGEntertain.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.FDGEntertain.data.db.AppDatabase
import com.FDGEntertain.data.db.ScoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DB_NAME = "gravity.db"
private const val SETTINGS_NAME = "settings"

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, DB_NAME).build()

    @Provides
    fun provideScoreDao(db: AppDatabase): ScoreDao = db.scoreDao()

    @Provides @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext ctx: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            produceFile = { ctx.preferencesDataStoreFile(SETTINGS_NAME) } // "settings.preferences_pb"
        )
}
