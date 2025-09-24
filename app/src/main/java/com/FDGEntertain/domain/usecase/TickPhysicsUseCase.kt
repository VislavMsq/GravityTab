package com.FDGEntertain.domain.usecase

import com.FDGEntertain.domain.model.Ball
import kotlin.math.min

class TickPhysicsUseCase {
    fun invoke(
        ball: Ball,
        dtSec: Float,
        accel: Float,
        maxVy: Float = 2400f,
        groundY: Float = 1000f,
    ): Pair<Ball, Boolean> {
        // КЛЮЧЕВОЕ: ограничиваем шаг (50мс) — больше нам не нужно
        val dt = dtSec.coerceIn(0f, 0.05f)

        val vy = min(ball.vy + accel * dt, maxVy)
        val y = ball.y + vy * dt

        val hitGround = y >= groundY
        val clampedY = if (hitGround) groundY else y

        return ball.copy(y = clampedY, vy = vy) to hitGround
    }
}
