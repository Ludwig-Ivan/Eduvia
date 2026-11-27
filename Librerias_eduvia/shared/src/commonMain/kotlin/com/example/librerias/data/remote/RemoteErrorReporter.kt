package com.example.librerias.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canal compartido por las cachés de listados para avisar de fallos de carga.
 *
 * Los flujos de datos conservan el último valor conocido (para que la UI no se quede vacía al
 * apagar la API), pero el error se reporta aquí y la navegación puede mostrar un aviso con
 * botón de reintento.
 */
class RemoteErrorReporter {
    private val _lastError = MutableStateFlow<Throwable?>(null)

    /** Último fallo de carga; `null` cuando la última carga o el reintento terminó bien. */
    val lastError: StateFlow<Throwable?> = _lastError.asStateFlow()

    fun report(error: Throwable) {
        _lastError.value = error
    }

    fun clear() {
        _lastError.value = null
    }
}
