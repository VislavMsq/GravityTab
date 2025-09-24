package com.FDGEntertain.util

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import kotlin.apply

class LogoAnimator(private val logoView: ImageView) {
    private var animator: ObjectAnimator? = null

    fun start() {
        if (animator?.isRunning == true) return

        val dy = 6f * logoView.resources.displayMetrics.density // сдвиг вверх ~6dp

        // ключевые кадры: масштаб, вертикальный сдвиг и прозрачность
        val pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.98f, 1.06f, 0.98f)
        val pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.98f, 1.06f, 0.98f)
        val pvhT = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -dy, 0f)
        val pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0.95f, 1f, 0.95f)

        animator = ObjectAnimator.ofPropertyValuesHolder(logoView, pvhX, pvhY, pvhT, pvhA).apply {
            duration = 1400L
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        logoView.apply {
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
            alpha = 1f
        }
    }
}
