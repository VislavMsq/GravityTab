package com.mosiuk.gravitytap.domain.game

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GameLoop(
    private val frameMs: Long = 16L,
) {
    fun ticks(): Flow<GameAction.Tick> =
        flow {
            var last = SystemClock.uptimeMillis()
            while (true) {
                val now = SystemClock.uptimeMillis()
                val dtMs = (now - last).coerceAtMost(50)
                last = now
                emit(GameAction.Tick(nowMs = now, dtSec = dtMs / 1000f))
                delay(frameMs) // ~60 FPS
            }
        }
}
