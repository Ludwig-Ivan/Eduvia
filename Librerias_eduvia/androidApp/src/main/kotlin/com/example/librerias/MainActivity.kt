package com.example.librerias // O el nombre de tu paquete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.librerias.di.AppContainer
import com.example.librerias.navigation.AppNavHost

/**
 * Punto de entrada de Android: crea el contenedor y delega la interfaz en el código compartido
 * (`AppNavHost` del módulo `shared`), sin lógica de UI propia.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Creamos el contenedor
            val container = remember { AppContainer() }

            // 2. Llamamos a la navegación
            AppNavHost(container = container)
        }
    }
}