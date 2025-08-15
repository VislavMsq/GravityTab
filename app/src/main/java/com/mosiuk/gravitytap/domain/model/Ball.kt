package com.mosiuk.gravitytap.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Пакет содержит модели предметной области приложения.
 * 
 * Включает в себя классы, представляющие игровые объекты и состояние игры.
 */

/**
 * Класс, представляющий шарик в игре.
 * 
 * @property column Номер колонки, в которой находится шарик (0..n).
 * @property y Текущая вертикальная координата шарика в пикселях.
 * @property vy Вертикальная скорость шарика в пикселях в секунду.
 * @property bornAt Временная метка (в миллисекундах) создания шарика.
 */
@Parcelize
data class Ball(
    val column: Int,
    val y: Float,
    val vy: Float,
    val bornAt: Long,
) : Parcelable {
    init {
        require(column >= 0) { "Номер колонки не может быть отрицательным" }
        require(y >= 0) { "Координата Y не может быть отрицательной" }
    }
}

/**
 * Класс, представляющий состояние игры в определенный момент времени.
 * 
 * @property difficulty Текущий уровень сложности игры.
 * @property score Текущее количество очков.
 * @property lives Количество оставшихся жизней игрока.
 * @property combo Текущая серия последовательных попаданий (комбо).
 * @property maxCombo Максимальное комбо, достигнутое за игру.
 * @property ball Текущий активный шарик или null, если шарик не активен.
 * @property isPaused Флаг, указывающий, находится ли игра в состоянии паузы.
 * @property elapsedMs Время, прошедшее с начала игры в миллисекундах.
 */
@Parcelize
data class GameState(
    val difficulty: Difficulty,
    val score: Int = 0,
    val lives: Int = difficulty.lives,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val ball: Ball? = null,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
) : Parcelable {
    init {
        require(score >= 0) { "Количество очков не может быть отрицательным" }
        require(lives >= 0) { "Количество жизней не может быть отрицательным" }
        require(combo >= 0) { "Комбо не может быть отрицательным" }
        require(maxCombo >= 0) { "Максимальное комбо не может быть отрицательным" }
        require(elapsedMs >= 0) { "Прошедшее время не может быть отрицательным" }
    }
    
    /**
     * Создает копию состояния с увеличенным количеством очков.
     * 
     * @param points Количество очков для добавления.
     * @return Новое состояние с обновленным счетом.
     */
    fun addScore(points: Int): GameState {
        require(points > 0) { "Количество очков должно быть положительным" }
        return copy(score = this.score + points)
    }
    
    /**
     * Создает копию состояния с уменьшенным количеством жизней.
     * 
     * @return Новое состояние с уменьшенным количеством жизней.
     */
    fun loseLife(): GameState {
        return copy(lives = this.lives - 1, combo = 0)
    }
    
    /**
     * Проверяет, завершилась ли игра (закончились жизни).
     * 
     * @return true, если игрок проиграл, иначе false.
     */
    fun isGameOver(): Boolean = lives <= 0
}
