package com.mosiuk.gravitytap.core.nav

sealed interface Route {
    @JvmInline value class Game(val difficulty: String) : Route
    data object Splash : Route
    data object Menu : Route
    data object Result : Route
}

object Destinations {
    const val Spalsh = "Splash"
    const val Menu = "Menu"
    const val Game = "Game/{difficulty}"
    const val Result = "Result"
}