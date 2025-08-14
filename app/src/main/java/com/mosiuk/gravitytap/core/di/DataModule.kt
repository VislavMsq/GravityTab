package com.mosiuk.gravitytap.core.di
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.mosiuk.gravitytap.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DB_NAME = "gravity.db"
private const val DS_NAME = "settings.pd"

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, DB_NAME).build()

    @Provides @Singleton
    fun provideScoreDao(db: AppDatabase) = db.scoreDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context) =
        PreferenceDataStoreFactory.create {
            ctx.preferencesDataStoreFile(DS_NAME)
        }
}