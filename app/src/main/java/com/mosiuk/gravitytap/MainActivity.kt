package com.mosiuk.gravitytap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.mosiuk.gravitytap.core.nav.AppNavHost
import com.mosiuk.gravitytap.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная активность приложения, являющаяся точкой входа в приложение.
 * Использует Hilt для внедрения зависимостей и Compose для UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Вызывается при создании активности.
     * Устанавливает корневой композейбл с навигацией и темой приложения.
     * 
     * @param savedInstanceState Сохраненное состояние активности, если оно есть.
     */
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Применяем тему приложения
            AppTheme {
                // Получаем класс размера окна для адаптивного дизайна
                val windowSizeClass = calculateWindowSizeClass(this)
                // Устанавливаем корневой навигационный хост
                AppNavHost(windowSizeClass)
            }
        }
    }
}
