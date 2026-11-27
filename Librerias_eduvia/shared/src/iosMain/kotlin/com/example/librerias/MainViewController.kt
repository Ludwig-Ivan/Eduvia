package com.example.librerias

import androidx.compose.ui.window.ComposeUIViewController
import com.example.librerias.di.AppContainer
import com.example.librerias.navigation.AppNavHost
import platform.UIKit.UIViewController

/**
 * Punto de entrada iOS: crea el `ComposeUIViewController` que aloja la interfaz Compose
 * de la aplicación; la base URL local se define
 * en `PlatformHttpClient.kt` de `iosMain`.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val container = AppContainer()
    AppNavHost(container = container)
}