package com.FDGEntertain

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Основной класс приложения, инициализирующий Hilt для внедрения зависимостей.
 * Наследуется от Application и помечается аннотацией @HiltAndroidApp,
 * что позволяет Hilt генерировать код для внедрения зависимостей.
 */
@HiltAndroidApp
class App : Application()
