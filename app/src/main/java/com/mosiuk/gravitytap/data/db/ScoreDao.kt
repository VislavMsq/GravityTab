package com.mosiuk.gravitytap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosiuk.gravitytap.domain.model.ScoreEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(score: ScoreEntry)

    @Query("SELECT * FROM ScoreEntry ORDER BY score DESC, date DESC LIMIT :limit")
    fun top(limit: Int = 20): Flow<List<ScoreEntry>>
}