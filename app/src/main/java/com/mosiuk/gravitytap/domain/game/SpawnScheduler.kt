package com.mosiuk.gravitytap.domain.game

class SpawnScheduler(
    next: Long,
) {
    private var nextSpawnAt = next

    fun shouldSpawn(
        now: Long,
        intervalMs: Long,
    ): Boolean {
        if (now < nextSpawnAt) return false
        nextSpawnAt = now + intervalMs
        return true
    }

    fun snapshot(): Long = nextSpawnAt

    fun restore(next: Long) {
        nextSpawnAt = next
    }
}
