package com.mosiuk.gravitytap.domain.model

import androidx.room.PrimaryKey

data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val score: Int,
    val difficulty: Int,
    val maxCombo: Int
)
