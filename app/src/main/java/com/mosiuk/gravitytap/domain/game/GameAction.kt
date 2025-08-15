package com.mosiuk.gravitytap.domain.game

sealed interface GameAction {
    data class Tick(
        val nowMs: Long,
        val dtSec: Float,
    ) : GameAction

    data class Spawn(
        val nowMs: Long,
    ) : GameAction

    data object Tap : GameAction

    data object PauseToggle : GameAction
}
