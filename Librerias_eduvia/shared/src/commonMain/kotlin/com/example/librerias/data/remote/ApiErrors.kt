package com.example.librerias.data.remote

import com.example.librerias.model.AppError
import com.example.librerias.model.AppErrorCarrier
import com.example.librerias.model.AppErrorKind
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Traduce cualquier fallo de la capa de red a un [AppError] seguro para la UI.
 *
 * Es el único lugar donde se decide el texto que ve el usuario: las excepciones de Ktor/OkHttp
 * (`unexpected end of stream`, `Socket timeout...`, etc.) nunca se muestran tal cual. Los mensajes
 * del backend se conservan solo cuando son texto legible en español y no parecen internos.
 */
internal object ApiErrors {

    /** Punto de entrada usado por repositorios y ViewModels. */
    fun from(error: Throwable): AppError = when {
        error is AppErrorCarrier -> error.appError
        error is ApiException -> fromApi(error)
        error is SerializationException -> AppError(
            AppErrorKind.UNEXPECTED,
            "No pudimos entender la respuesta del servidor. Intenta de nuevo.",
            retryable = true,
        )
        error.isTimeout() -> AppError(
            AppErrorKind.TIMEOUT,
            "La conexión tardó demasiado. Revisa tu internet e inténtalo de nuevo.",
            retryable = true,
        )
        error.isConnectivity() -> AppError(
            AppErrorKind.CONNECTION,
            "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.",
            retryable = true,
        )
        // Este mapper solo se usa en la capa de red: cualquier excepción no clasificada
        // significa que la petición no llegó a completarse. Mejor decir «sin conexión» que
        // mostrar un error inesperado y perder la pista de que se puede reintentar.
        else -> AppError(
            AppErrorKind.CONNECTION,
            "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.",
            retryable = true,
        )
    }

    /** Clasifica una respuesta HTTP no 2xx convertida por [ApiClient]. */
    private fun fromApi(error: ApiException): AppError {
        val detail = safeDetail(error.problem?.detail ?: error.message)
        return when (error.status) {
            401 -> if (error.path?.contains("auth/login") == true) {
                AppError(
                    AppErrorKind.CREDENTIALS,
                    "Correo o contraseña incorrectos. Revisa tus datos o recupera tu contraseña.",
                )
            } else {
                AppError(
                    AppErrorKind.SESSION,
                    "Tu sesión venció. Vuelve a iniciar sesión para continuar.",
                )
            }

            403 -> AppError(
                AppErrorKind.SESSION,
                "No tienes permiso para realizar esta acción.",
            )

            404 -> AppError(
                AppErrorKind.NOT_FOUND,
                detail ?: "No encontramos lo que buscabas. Puede que ya no esté disponible.",
            )

            409 -> AppError(
                AppErrorKind.CONFLICT,
                detail ?: "Ese registro ya existe. Revisa los datos e inténtalo de nuevo.",
            )

            in 400..499 -> {
                if (isCredentialDetail(detail)) {
                    AppError(
                        AppErrorKind.CREDENTIALS,
                        "Correo o contraseña incorrectos. Revisa tus datos o recupera tu contraseña.",
                    )
                } else {
                    AppError(
                        AppErrorKind.VALIDATION,
                        detail ?: "Revisa los datos e inténtalo de nuevo.",
                    )
                }
            }

            else -> AppError(
                AppErrorKind.SERVER,
                "El servidor no está disponible en este momento. Inténtalo de nuevo en unos minutos.",
                retryable = true,
            )
        }
    }

    /** El texto del backend solo se muestra si es corto y no filtra detalles técnicos. */
    private fun safeDetail(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank() || text.length > 160) return null
        val lower = text.lowercase()
        val technical = listOf(
            "http://", "https://", "exception", "socket", "timeout", "unexpected end",
            "stacktrace", "java.", "kotlin.", "connection reset", "at com.",
        )
        return if (technical.any { lower.contains(it) }) null else text
    }

    /** El backend puede responder 400 o 401 con Variantes de «credenciales inválidas». */
    private fun isCredentialDetail(detail: String?): Boolean {
        val text = detail?.lowercase().orEmpty()
        return text.contains("credencial") || text.contains("contraseña") ||
            text.contains("correo o contraseña") || text.contains("password")
    }

    /** Busca timeouts de Ktor, OkHttp o cualquier causa encadenada. */
    private fun Throwable.isTimeout(): Boolean = causeChain().any {
        it is ConnectTimeoutException || it is SocketTimeoutException ||
            it is HttpRequestTimeoutException ||
            it::class.simpleName?.contains("timeout", ignoreCase = true) == true ||
            run {
                val msg = it.message?.lowercase().orEmpty()
                msg.contains("timeout") || msg.contains("timed out")
            }
    }

    /** Red caída, API apagada, conexión rechazada o stream cortado. */
    private fun Throwable.isConnectivity(): Boolean = causeChain().any {
        it is IOException ||
            it::class.simpleName?.contains("ConnectException", ignoreCase = true) == true ||
            it::class.simpleName?.contains("UnknownHost", ignoreCase = true) == true ||
            run {
                val msg = it.message?.lowercase().orEmpty()
                msg.contains("failed to connect") || msg.contains("unexpected end of stream") ||
                    msg.contains("connection reset") || msg.contains("connection refused") ||
                    msg.contains("econnrefused") || msg.contains("could not connect")
            }
    }

    /** Incluye la causa raíz para mensajes como `SocketException` envueltos por Ktor. */
    private fun Throwable.causeChain(): List<Throwable> {
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = this
        while (current != null && seen.add(current)) current = current.cause
        return seen.toList()
    }
}
