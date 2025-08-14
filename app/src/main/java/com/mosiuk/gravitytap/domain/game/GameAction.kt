package com.mosiuk.gravitytap.domain.game

// domain/game/GameAction.kt
sealed interface GameAction {
    data class Tick(val nowMs: Long, val dtSec: Float) : GameAction
    data object Tap : GameAction
    data object PauseToggle : GameAction
    data class Spawn(val nowMs: Long) : GameAction
}
