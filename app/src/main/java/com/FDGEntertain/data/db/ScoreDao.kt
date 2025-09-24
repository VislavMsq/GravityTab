package com.FDGEntertain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.FDGEntertain.domain.model.ScoreEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для работы с таблицей рекордов в базе данных.
 * 
 * Определяет методы для доступа к данным, связанным с рекордами игроков,
 * включая вставку новых записей и получение списка лучших результатов.
 */

@Dao
interface ScoreDao {
    /**
     * Вставляет или обновляет запись о рекорде в базе данных.
     * 
     * @param score Объект [ScoreEntry], содержащий данные о рекорде.
     *              Если запись с таким же primary key уже существует, она будет заменена.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ScoreEntry)

    /**
     * Получает список лучших результатов из базы данных.
     * 
     * @param limit Максимальное количество возвращаемых записей (по умолчанию 20).
     * @return [Flow] со списком объектов [ScoreEntry], отсортированных по убыванию счета.
     *         В случае одинакового счета записи сортируются по дате (новые выше).
     *         Подписчики будут уведомлены об изменениях в реальном времени.
     */
    @Query("""
        SELECT * 
        FROM score_entries 
        ORDER BY score DESC, date DESC 
        LIMIT :limit
    """)
    fun top(limit: Int = 20): Flow<List<ScoreEntry>>
}
