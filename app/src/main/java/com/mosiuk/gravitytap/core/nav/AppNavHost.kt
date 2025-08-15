@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.mosiuk.gravitytap.core.nav

/**
 * Файл содержит основной навигационный хост приложения,
 * который управляет переходами между всеми экранами.
 * 
 * Основные особенности:
 * - Использует Compose Navigation для навигации
 * - Включает анимации переходов между экранами
 * - Управляет жизненным циклом ViewModel'ей
 * - Обрабатывает передачу данных между экранами
 */

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mosiuk.gravitytap.ui.game.GameScreen
import com.mosiuk.gravitytap.ui.menu.MenuScreen
import com.mosiuk.gravitytap.ui.result.ResultScreen
import com.mosiuk.gravitytap.ui.splash.SplashViewModel
import com.mosiuk.gravitytap.ui.vm.GameViewModel
import com.mosiuk.gravitytap.ui.vm.MenuViewModel
import com.mosiuk.gravitytap.ui.vm.ResultViewModel
import kotlinx.coroutines.launch

/**
 * Главный навигационный хост приложения, который определяет граф навигации
 * и управляет переходами между всеми экранами.
 * 
 * @param windowSizeClass Класс размера окна, используемый для адаптивного дизайна.
 *                       Влияет на макет экранов в зависимости от размера устройства.
 * 
 * Структура навигации:
 * 1. Splash Screen (ROUTE_SPLASH) - начальный экран с анимацией загрузки
 * 2. Главное меню (ROUTE_MENU) - основной экран с кнопками навигации
 * 3. Игровой экран (ROUTE_GAME) - экран с игровым процессом
 * 4. Экран результатов (ROUTE_RESULT) - отображение результатов игры и таблицы рекордов
 */
@Composable
fun AppNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Создаем NavHost с анимациями перехода между экранами
    // Используется для управления стеком навигации и анимациями переходов
    NavHost(
        navController = navController,
        startDestination = Destinations.ROUTE_SPLASH,
        // Анимации для плавных переходов между экранами
        enterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        exitTransition = { fadeOut() + scaleOut(targetScale = .98f) },
        popEnterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        popExitTransition = { fadeOut() + scaleOut(targetScale = .98f) },
    ) {
        /**
         * Определение экрана заставки (Splash Screen).
         * Этот экран отображается при запуске приложения и автоматически переходит в главное меню.
         */
        composable(Destinations.ROUTE_SPLASH) { backStackEntry ->
            // Инициализация ViewModel с областью видимости, привязанной к этому экрану
            val vm: SplashViewModel = hiltViewModel(backStackEntry)
            
            // Отображение экрана заставки
            com.mosiuk.gravitytap.ui.splash.SplashScreen(
                vm = vm,
                // Обработчик завершения анимации заставки
                onDone = {
                    // Навигация на главный экран с очисткой стека,
                    // чтобы нельзя было вернуться на сплэш-экран кнопкой "Назад"
                    navController.navigate(Destinations.ROUTE_MENU) {
                        launchSingleTop = true  // Предотвращает создание дубликатов
                        popUpTo(Destinations.ROUTE_SPLASH) { inclusive = true }  // Очистка стека
                    }
                },
            )
        }

        /**
         * Определение главного экрана меню приложения.
         * Содержит кнопки для начала игры и просмотра таблицы рекордов.
         */
        composable(Destinations.ROUTE_MENU) {
            // Инициализация ViewModel для главного меню
            val vm: MenuViewModel = hiltViewModel()
            
            // Отображение главного меню
            MenuScreen(
                windowSizeClass = windowSizeClass,
                // Обработчик нажатия кнопки "Начать игру"
                onStart = { difficulty ->
                    scope.launch {
                        // Навигация на экран игры с передачей выбранного уровня сложности
                        navController.navigate("game/$difficulty") {
                            launchSingleTop = true  // Предотвращает создание дубликатов экрана
                        }
                    }
                },
                // Обработчик нажатия кнопки "Таблица рекордов"
                onScores = {
                    scope.launch {
                        // Навигация на экран результатов с параметрами по умолчанию
                        navController.navigate(Destinations.ROUTE_RESULT) {
                            launchSingleTop = true  // Предотвращает создание дубликатов экрана
                        }
                    }
                },
                // Передача ViewModel в UI
                vm = vm,
            )
        }

        /**
         * Определение игрового экрана.
         * Принимает параметр сложности и отображает игровой процесс.
         * 
         * @param difficulty Уровень сложности игры (легкий, средний, сложный)
         */
        composable(
            route = Destinations.ROUTE_GAME,
            arguments = listOf(
                navArgument("difficulty") { 
                    type = NavType.StringType  // Тип параметра - строка
                    // Можно добавить валидацию или значение по умолчанию при необходимости
                }
            ),
        ) { backStackEntry ->
            // Инициализация ViewModel с областью видимости, привязанной к этому экрану
            val vm: GameViewModel = hiltViewModel(backStackEntry)
            
            // Отображение игрового экрана
            GameScreen(
                vm = vm,
                windowSizeClass = windowSizeClass,
                // Обработчик завершения игры
                onFinish = { score, diff, maxCombo ->
                    scope.launch {
                        // Навигация на экран результатов с передачей параметров игры
                        navController.navigate(Destinations.result(score, diff, maxCombo)) {
                            launchSingleTop = true  // Предотвращает создание дубликатов
                            // Очистка стека до главного меню, но оставляем меню в стеке
                            popUpTo(Destinations.ROUTE_MENU) { inclusive = false }
                        }
                    }
                },
            )
        }

        /**
         * Определение экрана результатов игры.
         * Отображает итоги последней игры и таблицу рекордов.
         * 
         * @param score Количество набранных очков (по умолчанию 0)
         * @param difficulty Уровень сложности (по умолчанию NORMAL)
         * @param maxCombo Максимальное комбо в игре (по умолчанию 0)
         */
        composable(
            route = Destinations.ROUTE_RESULT,
            arguments = listOf(
                // Параметр: количество очков
                navArgument("score") { 
                    type = NavType.IntType
                    defaultValue = 0  // Значение по умолчанию, если параметр не передан
                },
                // Параметр: уровень сложности
                navArgument("difficulty") { 
                    type = NavType.StringType
                    defaultValue = "NORMAL"  // Значение по умолчанию
                },
                // Параметр: максимальное комбо
                navArgument("maxCombo") { 
                    type = NavType.IntType
                    defaultValue = 0  // Значение по умолчанию
                },
            ),
        ) { backStackEntry ->
            // Инициализация ViewModel с областью видимости, привязанной к этому экрану
            val vm: ResultViewModel = hiltViewModel(backStackEntry)
            
            // Отображение экрана результатов
            ResultScreen(
                vm = vm,
                windowSizeClass = windowSizeClass,
                // Обработчик нажатия кнопки "Играть снова"
                onPlayAgain = {
                    scope.launch {
                        // Возврат в главное меню с очисткой стека навигации
                        navController.navigate(Destinations.ROUTE_MENU) {
                            launchSingleTop = true  // Предотвращает создание дубликатов
                            popUpTo(Destinations.ROUTE_MENU) { inclusive = true }  // Очистка стека
                        }
                    }
                },
            )
        }
    }
}
