package com.mosiuk.gravitytap.domain.model

enum class Difficulty(val spawnsMs: Int, val accelMs: Float, val lives: Int) {
    EASY(spawnsMs = 1200, accelMs = 900f, lives = 5),
    NORMAL(spawnsMs = 900, accelMs = 1100f, lives = 4),
    HARD(spawnsMs = 700, accelMs = 1350f, lives = 3);

    companion object {
        fun from(s: String) = entries.firstOrNull { it.name == s } ?: NORMAL
    }
}
