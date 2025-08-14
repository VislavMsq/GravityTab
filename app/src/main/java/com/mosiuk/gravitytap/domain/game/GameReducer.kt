package com.mosiuk.gravitytap.domain.game

import com.mosiuk.gravitytap.domain.model.GameState
import com.mosiuk.gravitytap.domain.usecase.SpawnBallUseCase
import com.mosiuk.gravitytap.domain.usecase.TickPhysicsUseCase
import com.mosiuk.gravitytap.domain.usecase.UpdateScoreOnHitUseCase

data class ReduceResult(
    val state: GameState,
    val effect: GameEffect? = null
)

/**
 * Чистая функция: (state, action) -> (новый state, optional effect)
 * НЕТ корутин, времени, SavedStateHandle, Dispatcher'ов и т.п.
 */
class GameReducer(
    private val tick: TickPhysicsUseCase,
    private val score: UpdateScoreOnHitUseCase,
    private val spawn: SpawnBallUseCase,
    private val groundY: Float = 1000f
) {
    fun reduce(s: GameState, a: GameAction): ReduceResult = when (a) {
        is GameAction.PauseToggle -> ReduceResult(s.copy(isPaused = !s.isPaused))

        is GameAction.Tap ->
            if (s.isPaused || s.ball == null) {
                ReduceResult(s)
            } else {
                val out = score.onHit(s.score, s.combo, s.maxCombo)
                val ns = s.copy(
                    score = out.scoreDelta,
                    combo = out.newCombo,
                    maxCombo = out.maxCombo,
                    ball = null
                )
                ReduceResult(ns)
            }

        is GameAction.Spawn ->
            if (s.isPaused || s.ball != null) {
                ReduceResult(s)
            } else {
                ReduceResult(s.copy(ball = spawn.invoke(a.nowMs)))
            }

        is GameAction.Tick ->
            if (s.isPaused || s.ball == null) {
                ReduceResult(s)
            } else {
                val (nb, hitGround) = tick.invoke(
                    s.ball,
                    a.dtSec,
                    s.difficulty.accelMs,
                    groundY = groundY
                )
                val afterFall = s.copy(ball = if (hitGround) null else nb)
                if (!hitGround) {
                    ReduceResult(afterFall)
                } else {
                    val miss = score.onMiss(afterFall.score, afterFall.combo, afterFall.maxCombo)
                    val damaged = afterFall.copy(
                        score = miss.scoreDelta,
                        combo = miss.newCombo,
                        maxCombo = miss.maxCombo,
                        lives = afterFall.lives - 1
                    )
                    if (damaged.lives <= 0) ReduceResult(
                        damaged,
                        GameEffect.GameOver
                    ) else ReduceResult(damaged)
                }
            }
    }
}

