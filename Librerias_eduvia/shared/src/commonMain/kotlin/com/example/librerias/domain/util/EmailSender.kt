package com.example.librerias.domain.util

/**
 * Abstrae el envío de correos. En producción se implementaría con tu backend.
 *
 * Existe para que `SendResetCodeUseCase` no dependa de Android ni de un servicio de correo:
 * en la demo `FakeEmailSender` no envía nada y deja el mensaje en una bandeja que la pantalla
 * puede mostrar, y en tests se puede sustituir por un doble que registre los códigos enviados.
 */
interface EmailSender {
    /**
     * Entrega el [code] de recuperación a [toEmail].
     * @param toEmail destinatario del correo.
     * @param code código de 6 dígitos generado por el repositorio de autenticación.
     * @return sin valor; la abstracción no informa de si el correo llegó.
     */
    suspend fun sendResetCode(toEmail: String, code: String)
}
