package com.mosiuk.gravitytap.core.nav

import android.os.Build
import androidx.annotation.RequiresApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Файл, содержащий определения маршрутов навигации приложения.
 * Определяет структуру URL и параметры для каждого экрана.
 */

/**
 * Запечатанный интерфейс, представляющий маршруты навигации в приложении.
 * Каждый наследник представляет отдельный экран или состояние навигации.
 */
sealed interface Route {
    /**
     * Маршрут экрана игры с параметром сложности.
     * @property difficulty Уровень сложности игры
     */
    @JvmInline
    value class Game(
        val difficulty: String,
    ) : Route

    /** Маршрут экрана заставки */
    data object Splash : Route

    /** Маршрут главного меню */
    data object Menu : Route

    /** Маршрут экрана результатов */
    data object Result : Route
}

/**
 * Объект, содержащий константы и утилитные методы для работы с маршрутами.
 * Определяет шаблоны URL и методы их генерации.
 */
object Destinations {
    /** URL экрана заставки */
    const val ROUTE_SPLASH = "splash"
    
    /** URL главного меню */
    const val ROUTE_MENU = "menu"
    
    /** Шаблон URL экрана игры с параметром сложности */
    const val ROUTE_GAME = "game/{difficulty}"
    
    /** Шаблон URL экрана результатов с параметрами счета, сложности и максимального комбо */
    const val ROUTE_RESULT =
        "result?score={score}&difficulty={difficulty}&maxCombo={maxCombo}"

    /**
     * Создает URL для перехода на экран результатов с заданными параметрами.
     * 
     * @param score Количество очков
     * @param difficulty Уровень сложности
     * @param maxCombo Максимальное комбо в игре
     * @return Строка URL с закодированными параметрами
     */
    fun result(score: Int, difficulty: String, maxCombo: Int): String {
        // Кодируем сложность для безопасного использования в URL
        val d = URLEncoder.encode(difficulty, StandardCharsets.UTF_8.toString())
        return "result?score=$score&difficulty=$d&maxCombo=$maxCombo"
    }
}
