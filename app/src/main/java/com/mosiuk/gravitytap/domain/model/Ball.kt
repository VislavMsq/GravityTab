package com.mosiuk.gravitytap.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ball(
    val column: Int,
    val y: Float,
    val vy: Float,
    val bornAt: Long,
) : Parcelable

@Parcelize
data class GameState(
    val difficulty: Difficulty,
    val score: Int = 0,
    val lives: Int = difficulty.lives,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val ball: Ball? = null,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
) : Parcelable
