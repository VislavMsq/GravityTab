@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.mosiuk.gravitytap.core.nav

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

@Composable
fun AppNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Destinations.ROUTE_SPLASH,
        enterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        exitTransition = { fadeOut() + scaleOut(targetScale = .98f) },
        popEnterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        popExitTransition = { fadeOut() + scaleOut(targetScale = .98f) },
    ) {
        composable(Destinations.ROUTE_SPLASH) { backStackEntry ->
            val vm: SplashViewModel = hiltViewModel(backStackEntry)
            com.mosiuk.gravitytap.ui.splash.SplashScreen(
                vm = vm,
                onDone = {
                    navController.navigate(Destinations.ROUTE_MENU) {
                        launchSingleTop = true
                        popUpTo(Destinations.ROUTE_SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.ROUTE_MENU) {
            val vm: MenuViewModel = hiltViewModel()
            MenuScreen(
                windowSizeClass = windowSizeClass,
                onStart = { difficulty ->
                    scope.launch {
                        navController.navigate("game/$difficulty") {
                            launchSingleTop = true
                        }
                    }
                },
                onScores = {
                    scope.launch {
                        navController.navigate(Destinations.ROUTE_RESULT) {
                            launchSingleTop = true
                        }
                    }
                },
                vm = vm,
            )
        }

        composable(
            route = Destinations.ROUTE_GAME,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: GameViewModel = hiltViewModel(backStackEntry)
            GameScreen(
                vm = vm,
                windowSizeClass = windowSizeClass,
                onFinish = { score, diff, maxCombo ->
                    scope.launch {
                        navController.navigate(Destinations.result(score, diff, maxCombo)) {
                            launchSingleTop = true
                            popUpTo(Destinations.ROUTE_MENU) { inclusive = false }
                        }
                    }
                },
            )
        }

        composable(
            route = Destinations.ROUTE_RESULT,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType; defaultValue = 0 },
                navArgument("difficulty") { type = NavType.StringType; defaultValue = "NORMAL" },
                navArgument("maxCombo") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            val vm: ResultViewModel = hiltViewModel(backStackEntry)
            ResultScreen(
                vm = vm,
                windowSizeClass = windowSizeClass,
                onPlayAgain = {
                    scope.launch {
                        navController.navigate(Destinations.ROUTE_MENU) {
                            launchSingleTop = true
                            popUpTo(Destinations.ROUTE_MENU) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}
