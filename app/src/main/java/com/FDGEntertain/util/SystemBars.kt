package com.FDGEntertain.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun ApplySystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkStatusIcons: Boolean,
    darkNavIcons: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)

        val prevStatus = window.statusBarColor
        val prevNav = window.navigationBarColor
        val prevLightStatus = controller.isAppearanceLightStatusBars
        val prevLightNav = controller.isAppearanceLightNavigationBars

        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
        controller.isAppearanceLightStatusBars = darkStatusIcons   // true = тёмные иконки
        controller.isAppearanceLightNavigationBars = darkNavIcons

        onDispose {
            window.statusBarColor = prevStatus
            window.navigationBarColor = prevNav
            controller.isAppearanceLightStatusBars = prevLightStatus
            controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }
}

class FcmTokenFetcher {
    fun fetch(onToken: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val frbToken = if (task.isSuccessful) task.result ?: "" else ""
            onToken(frbToken)
        }
    }
}

class StartUrlBuilder(
    private val baseUrl: String,
    private val packageName: String
) {
    /** Сборка URL из refer, frbToken и adId (порядок параметров сохранён). */
    fun buildStartUrl(refer: String, frbToken: String, adId: String): String {
        val query = buildQuery(
            mapOf(
                "flipAdNest" to adId,
                "matchReferCode" to refer,
                "wingFrbToken" to frbToken,
                "comboEggPack" to packageName
            )
        )
        return appendQuery(baseUrl, query)
    }

    // Делает "a=1&b=2" (без percent-encoding — ровно как у тебя было)
    private fun buildQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { "${it.key}=${it.value}" }

    // Если в baseUrl уже есть '?', приклеим через '&', иначе через '?'
    private fun appendQuery(baseUrl: String, query: String): String =
        if (baseUrl.contains("?")) "$baseUrl&$query" else "$baseUrl?$query"
}
