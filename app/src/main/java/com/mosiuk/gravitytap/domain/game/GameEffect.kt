package com.mosiuk.gravitytap.domain.game

sealed interface GameEffect {
    data object GameOver : GameEffect
}