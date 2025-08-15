// core/util/SoundManager.kt
package com.mosiuk.gravitytap.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SoundManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val soundPool: SoundPool
    private val hitId: Int
    private val missId: Int
    @Volatile private var loaded = false
    @Volatile private var muted = false   // <-- NEW

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loaded = true
        }

        hitId = soundPool.load(ctx, com.mosiuk.gravitytap.R.raw.hit, 1)
        missId = soundPool.load(ctx, com.mosiuk.gravitytap.R.raw.miss, 1)
    }

    fun setMuted(value: Boolean) { muted = value }       // <-- NEW
    fun stopAll() { soundPool.autoPause() }              // <-- NEW

    fun playHit(enabled: Boolean) {
        if (enabled && !muted && loaded) {
            soundPool.play(hitId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playMiss(enabled: Boolean) {
        if (enabled && !muted && loaded) {
            soundPool.play(missId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() = soundPool.release()
}
