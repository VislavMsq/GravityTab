package com.mosiuk.gravitytap.core.nav

import android.os.Build
import androidx.annotation.RequiresApi

sealed interface Route {
    @JvmInline value class Game(
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
    const val ROUTE_RESULT = "result?score={score}&difficulty={difficulty}&maxCombo={maxCombo}"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun result(score: Int, difficulty: String, maxCombo: Int): String =
        "result?score=$score&difficulty=${java.net.URLEncoder.encode(difficulty, Charsets.UTF_8)}&maxCombo=$maxCombo"
}
