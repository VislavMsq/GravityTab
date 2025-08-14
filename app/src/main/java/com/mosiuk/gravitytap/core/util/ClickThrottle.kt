package com.mosiuk.gravitytap.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClickThrottle(
    private val windowMs: Long = 600
) {
    private var last = 0L
    private val m = Mutex()

    suspend fun allow(): Boolean = m.withLock {
        val now = System.currentTimeMillis()
        return if (now - last > windowMs) {
            last = now;
            true
        } else {
            false
        }
    }
}