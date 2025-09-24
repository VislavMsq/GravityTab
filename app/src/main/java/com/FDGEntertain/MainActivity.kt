package com.FDGEntertain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.FDGEntertain.core.nav.AppNavHost
import com.FDGEntertain.ui.theme.GravityTapTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная активность приложения, являющаяся точкой входа в приложение.
 * Использует Hilt для внедрения зависимостей и Compose для UI.
 */
@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    /**
     * Вызывается при создании активности.
     * Устанавливает корневой композейбл с навигацией и темой приложения.
     * 
     * @param savedInstanceState Сохраненное состояние активности, если оно есть.
     */
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Используйте GravityTapTheme вместо AppTheme
            GravityTapTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                AppNavHost(windowSizeClass)
            }
        }
    }
}
