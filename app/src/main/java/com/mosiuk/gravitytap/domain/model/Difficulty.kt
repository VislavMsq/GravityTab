package com.mosiuk.gravitytap.domain.model

enum class Difficulty(
    val accel: Float,
    val spawnMs: Long,
    val lives: Int,
) {
    EASY(accel = 900f, spawnMs = 900L, lives = 5),
    NORMAL(accel = 1200f, spawnMs = 700L, lives = 3),
    HARD(accel = 1500f, spawnMs = 550L, lives = 2),
}
