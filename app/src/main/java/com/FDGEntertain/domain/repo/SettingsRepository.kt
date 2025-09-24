package com.FDGEntertain.domain.repo

import com.FDGEntertain.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val difficultyAndSound: Flow<Pair<Difficulty, Boolean>>
//    fun observe(): Flow<Pair<Difficulty, Boolean>>

    suspend fun updateDifficulty(d: Difficulty)

    suspend fun updateSound(sound: Boolean)
}
