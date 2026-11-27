package com.example.librerias.model

/** Categoría del fallo. La UI la usa para decidir si puede reintentar o si debe pedir otra acción. */
enum class AppErrorKind {
    /** No hay red, el servidor no está levantado o se cortó la conexión. */
    CONNECTION,

    /** La petición tardó más de lo permitido. */
    TIMEOUT,

    /** El servidor respondió con un error 5xx. */
    SERVER,

    /** La sesión venció o el usuario no tiene permiso. */
    SESSION,

    /** Usuario y contraseña no coinciden. */
    CREDENTIALS,

    /** El usuario debe corregir lo que escribió. */
    VALIDATION,

    /** El recurso solicitado no existe. */
    NOT_FOUND,

    /** El registro ya existe o contradice otro existente. */
    CONFLICT,

    /** Fallo no clasificado. */
    UNEXPECTED,
}

/**
 * Error listo para mostrar al usuario.
 *
 * [message] siempre es un texto en español y seguro para la UI: nunca contiene URLs internas,
 * stack traces ni mensajes crudos de Ktor/Java. [retryable] indica si tiene sentido ofrecer un
 * botón de reintento.
 */
data class AppError(
    val kind: AppErrorKind,
    val message: String,
    val retryable: Boolean = false,
)

/** Implementado por excepciones que ya traen un [AppError] calculado (por ejemplo, fallos de carga). */
interface AppErrorCarrier {
    val appError: AppError
}

/** Excepción de dominio para errores que llegan a la UI fuera de un `AppResult`. */
class AppFailureException(override val appError: AppError) :
    RuntimeException(appError.message), AppErrorCarrier
