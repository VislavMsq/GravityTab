package com.mosiuk.gravitytap.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosiuk.gravitytap.R

/**
 * Экран заставки приложения, отображаемый при запуске.
 * 
 * @param vm ViewModel экрана заставки, управляющий состоянием и логикой
 * @param onDone Колбэк, вызываемый по завершении загрузки и анимации
 */
@Composable
fun SplashScreen(
    vm: SplashViewModel,
    onDone: () -> Unit,
) {
    // Собираем состояние UI из ViewModel
    val ui by vm.ui.collectAsState()

    // Обработка эффектов от ViewModel
    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            // При получении эффекта навигации в меню вызываем колбэк onDone
            if (effect is SplashEffect.NavigateToMenu) onDone()
        }
    }

    // Основной контейнер экрана
    Scaffold { innerPadding ->
        // Вертикальный контейнер для центрирования контента
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // Учитываем системные отступы (вырез, жесты навигации и т.д.)
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Анимированное появление названия приложения
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(spring(dampingRatio = 0.8f)), // Плавное появление с пружинной анимацией
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp, // Увеличиваем межбуквенное расстояние для лучшей читаемости
                        ),
                )
            }
            
            // Отступ между названием и индикатором загрузки
            Spacer(Modifier.height(24.dp))

            // Индикатор загрузки с прогрессом из ViewModel
            CircularProgressIndicator(progress = { ui.progress })
            
            // Текст под индикатором загрузки
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.splash_loading))
        }
    }
}
