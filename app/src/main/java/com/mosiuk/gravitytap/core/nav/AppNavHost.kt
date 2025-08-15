// app/src/main/java/com/mosiuk/gravitytap/core/nav/AppNavHost.kt
package com.mosiuk.gravitytap.core.nav

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.mosiuk.gravitytap.ui.splash.SplashScreen
import com.mosiuk.gravitytap.ui.vm.GameViewModel
import com.mosiuk.gravitytap.ui.vm.MenuViewModel
import com.mosiuk.gravitytap.ui.vm.ResultViewModel
import com.mosiuk.gravitytap.ui.vm.SplashViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Destinations.ROUTE_SPLASH, // ← используем из Route.kt
        enterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        exitTransition = { fadeOut() + scaleOut(targetScale = .98f) },
    ) {
        composable(Destinations.ROUTE_SPLASH) {
            val vm: SplashViewModel = hiltViewModel()
            SplashScreen(
                onLoaded = {
                    scope.launch {
                        navController.navigate(Destinations.ROUTE_MENU) {
                            launchSingleTop = true
                            popUpTo(Destinations.ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Destinations.ROUTE_MENU) {
            val vm: MenuViewModel = hiltViewModel()
            MenuScreen(
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
            )
        }

        composable(
            route = Destinations.ROUTE_GAME,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: GameViewModel = hiltViewModel()
            GameScreen(
                vm = vm,
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
            arguments =
                listOf(
                    navArgument("score") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("difficulty") {
                        type = NavType.StringType
                        defaultValue = "NORMAL"
                    },
                    navArgument("maxCombo") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
        ) { backStackEntry ->
            val vm: ResultViewModel = hiltViewModel(backStackEntry)
            ResultScreen(
                vm = vm,
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
