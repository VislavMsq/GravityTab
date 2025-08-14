// TickPhysicsUseCase.kt
package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.Ball
import kotlin.math.min

class TickPhysicsUseCase {
    fun invoke(ball: Ball, dtSec: Float, accel: Float, maxVy: Float = 2400f, groundY: Float = 1000f)
            : Pair<Ball, Boolean> {
        val vy = min(ball.vy + accel * dtSec, maxVy)
        val y = ball.y + vy * dtSec
        val hitGround = y >= groundY
        val clampedY = if (hitGround) groundY else y
        return ball.copy(y = clampedY, vy = vy) to hitGround
    }
}
