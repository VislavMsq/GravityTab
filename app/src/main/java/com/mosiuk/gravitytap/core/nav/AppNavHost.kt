package com.mosiuk.gravitytap.core.nav

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.mosiuk.gravitytap.core.util.ClickThrottle
import com.mosiuk.gravitytap.ui.menu.MenuScreen
import com.mosiuk.gravitytap.ui.splash.SplashScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    val scope = rememberCoroutineScope()

    val throttle = remember { ClickThrottle() }

    NavHost(
        navController = navController,
        startDestination = Destinations.Spalsh,
        enterTransition = { fadeIn() + scaleIn(initialScale = .98f) },
        exitTransition = { fadeOut() + scaleOut(targetScale = .98f) }
    ) {
        composable(Destinations.Spalsh) {
            val vm = hiltViewModel<com.mosiuk.gravitytap.ui.vm.SplashViewModel>()
        }

        SplashScreen(
            onLoaded = {
                scope.launch {
                    if (throttle.allow()) navController.navigate(Destinations.Menu) {
                        launchSingleTop = true
                        popUpTo(Destinations.Spalsh) {
                            inclusive = true
                        }
                    }
                }
            }
        )
    }
    composable(Destinations.Menu) {
        val vm = hiltViewModel<com.mosiuk.gravitytap.ui.vm.MenuViewModel>()
        MenuScreen(
            onStart = {
                scope.launch {
                    if (throttle.allow()) navController.navigate("game/$difficulty") {
                        launchSingleTop = true
                    }
                }
            },
            onScores = {
                scope.launch {
                    if (throttle.allow()) navController.navigate(Destinations.Result) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }
    composable(
        route = "game/{difficulty}",
        arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
    ){
        val vm = hiltViewModel<com.mosiuk.gravitytap.ui.vm.GameViewModel>()
        GameScreen(
            vm = vm,
            onFinish = {
                scope.launch {
                    if (throttle.allow()) navController.navigate(Destinations.Result) {
                        launchSingleTop = true
                        popUpTo(Destinations.Menu) {
                            inclusive = true
                    }
                }
            }
        )
    }
    composable(Destinations.Result) {
        val vm = hiltViewModel<com.mosiuk.gravitytap.ui.vm.ResultViewModel>()
                ResultScreen(
                    vm = vm,
                    onPlayAgain = {
                        scope.launch {
                            if (throttle.allow()) navController.navigate(Destinations.Menu) {
                                launchSingleTop = true
                                popUpTo(Destinations.Menu) {
                                    inclusive = true
                                }
                            }
                        }
                    }
            }
}