package com.mosiuk.gravitytap.core.util

import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClickThrottle(
    private val windowMs: Long = 600,
    private val now: () -> Long = { SystemClock.uptimeMillis() },
) {
    private var last = 0L
    private val m = Mutex()

    suspend fun allow(): Boolean =
        m.withLock {
            val t = now()
            if (t - last > windowMs) {
                last = t
                true
            } else {
                false
            }
        }
}
