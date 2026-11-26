package com.example.librerias.model

/** Resultado de un caso de uso: éxito o error con mensaje listo para mostrar en la UI. */
sealed interface AppResult<out T> {
    /** Operación correcta: expone el valor devuelto por el caso de uso. */
    data class Success<out T>(val value: T) : AppResult<T>

    /** Operación fallida: expone un mensaje listo para mostrar en la UI. */
    data class Error(val message: String) : AppResult<Nothing>
}