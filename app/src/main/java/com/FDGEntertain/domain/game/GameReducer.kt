package com.FDGEntertain.domain.game

import com.FDGEntertain.domain.model.GameState
import com.FDGEntertain.domain.usecase.SpawnBallUseCase
import com.FDGEntertain.domain.usecase.TickPhysicsUseCase
import com.FDGEntertain.domain.usecase.UpdateScoreOnHitUseCase


data class ReduceResult(
    val state: GameState,
    val effect: GameEffect? = null,
)

class GameReducer(
    private val tick: TickPhysicsUseCase,
    private val score: UpdateScoreOnHitUseCase,
    private val spawn: SpawnBallUseCase,
    private val groundY: Float = 1000f,
) {
    fun reduce(
        s: GameState,
        a: GameAction,
    ): ReduceResult =
        when (a) {
            is GameAction.PauseToggle ->
                ReduceResult(s.copy(isPaused = !s.isPaused))

            is GameAction.Tap ->
                if (s.isPaused || s.ball == null) {
                    ReduceResult(s)
                } else {
                    val out = score.onHit(s.score, s.combo, s.maxCombo)
                    ReduceResult(
                        s.copy(
                            score = out.scoreDelta,
                            combo = out.newCombo,
                            maxCombo = out.maxCombo,
                            ball = null,
                        ),
                    )
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
                    val (nb, hitGround) =
                        tick.invoke(
                            ball = s.ball,
                            dtSec = a.dtSec,
                            accel = s.difficulty.accel, // проверь имя поля
                            groundY = groundY,
                        )
                    val afterFall = s.copy(ball = if (hitGround) null else nb)
                    if (!hitGround) {
                        ReduceResult(afterFall)
                    } else {
                        val miss = score.onMiss(afterFall.score, afterFall.combo, afterFall.maxCombo)
                        val damaged =
                            afterFall.copy(
                                score = miss.scoreDelta,
                                combo = miss.newCombo,
                                maxCombo = miss.maxCombo,
                                lives = afterFall.lives - 1,
                            )
                        if (damaged.lives <= 0) {
                            ReduceResult(
                                damaged,
                                GameEffect.GameOver(
                                    score = damaged.score,
                                    difficulty = damaged.difficulty,
                                    maxCombo = damaged.maxCombo,
                                ),
                            )
                        } else {
                            ReduceResult(damaged)
                        }
                    }
                }
        }
}
