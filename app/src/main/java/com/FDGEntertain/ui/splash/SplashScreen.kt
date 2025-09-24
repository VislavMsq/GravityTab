package com.FDGEntertain.ui.splash

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FDGEntertain.R
import com.FDGEntertain.util.ApplySystemBars
import com.mosiuk.FDGEntertain.ui.splash.SplashEffect
import com.mosiuk.FDGEntertain.ui.splash.SplashViewModel

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

    val cfg = LocalConfiguration.current
    val isDarkSystem =
        (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    ApplySystemBars(
        statusBarColor = if (isDarkSystem) Color.Black else Color.White,
        navigationBarColor = if (isDarkSystem) Color.Black else Color.White,
        darkStatusIcons = !isDarkSystem, // тёмные иконки на светлом баре
        darkNavIcons = !isDarkSystem
    )
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
    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { _ ->
        // Вертикальный контейнер для центрирования контента
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Контентная зона с отступами под системные бары
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()) // СНАЧАЛА инсет-паддинги
                    .paint(                                             // ПОТОМ фон-картинка в контенте
                        painterResource(R.drawable.load1),
                        contentScale = ContentScale.Crop
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Анимированное появление названия приложения
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(spring(dampingRatio = 0.8f)),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
                            ),
                        )

                        Spacer(Modifier.height(24.dp))

                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { ui.progress },
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = Color.White.copy(alpha = 0.35f),
                                strokeWidth = 4.dp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(40.dp)              // было height(40.dp) — лучше size
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.splash_loading),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

