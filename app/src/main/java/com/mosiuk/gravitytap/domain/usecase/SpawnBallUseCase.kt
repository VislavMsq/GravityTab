package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.Ball

class SpawnBallUseCase {
    operator fun invoke(now: Long): Ball {
        val col = kotlin.random.Random.nextInt(0, 3)
        return Ball(column = col, y = 0f, vy = 0f, bornAt = now)
    }
}
