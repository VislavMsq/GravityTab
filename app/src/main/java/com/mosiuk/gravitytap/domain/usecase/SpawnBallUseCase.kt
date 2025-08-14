package com.mosiuk.gravitytap.domain.usecase

import com.mosiuk.gravitytap.domain.model.Ball

class SpawnBallUseCase {
    fun invoke(now: Long, columns: Int = 3): Ball =
        Ball(column = (0 until columns).random(), y = 0f, vy = 0f, bornAt = now)

}