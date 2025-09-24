package com.FDGEntertain.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.FDGEntertain.domain.model.ScoreEntry

/**
 * Основной класс базы данных приложения, использующий Room для хранения данных.
 * 
 * Этот класс предоставляет доступ к DAO (Data Access Objects) для работы с данными.
 * Используется для хранения рекордов игроков и другой информации, требующей сохранения.
 */

/**
 * Абстрактный класс базы данных, расширяющий RoomDatabase.
 * 
 * @property entities Массив классов сущностей, которые будут храниться в базе данных.
 *                   В данном случае только [ScoreEntry] для хранения рекордов игроков.
 * @property version Версия базы данных. При изменении схемы базы данных необходимо увеличивать это значение.
 * @property exportSchema Флаг, указывающий, следует ли экспортировать схему базы данных в папку assets.
 */
@Database(
    entities = [ScoreEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Возвращает DAO (Data Access Object) для работы с таблицей рекордов.
     * 
     * @return Реализацию [ScoreDao] для выполнения операций с рекордами.
     */
    abstract fun scoreDao(): ScoreDao
}
