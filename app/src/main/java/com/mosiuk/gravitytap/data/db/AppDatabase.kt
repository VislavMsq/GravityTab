package com.mosiuk.gravitytap.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mosiuk.gravitytap.domain.model.ScoreEntry

@Database(entities = [ScoreEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}
