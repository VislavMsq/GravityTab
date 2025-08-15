package com.mosiuk.gravitytap.core.nav

import android.os.Build
import androidx.annotation.RequiresApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface Route {
    @JvmInline
    value class Game(
        val difficulty: String,
    ) : Route

    data object Splash : Route

    data object Menu : Route

    data object Result : Route
}

object Destinations {
    const val ROUTE_SPLASH = "splash"
    const val ROUTE_MENU = "menu"
    const val ROUTE_GAME = "game/{difficulty}"
    const val ROUTE_RESULT =
        "result?score={score}&difficulty={difficulty}&maxCombo={maxCombo}"

    fun result(score: Int, difficulty: String, maxCombo: Int): String {
        val d = URLEncoder.encode(difficulty, StandardCharsets.UTF_8.toString())
        return "result?score=$score&difficulty=$d&maxCombo=$maxCombo"
    }
}
