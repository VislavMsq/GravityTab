package com.mosiuk.gravitytap.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_entries")
data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val score: Int,
    val difficulty: String,
    val maxCombo: Int,
)
