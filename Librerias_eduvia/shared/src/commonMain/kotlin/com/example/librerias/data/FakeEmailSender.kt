package com.example.librerias.data

import com.example.librerias.domain.util.EmailSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Demo: no manda correo; deja el mensaje en una "bandeja" que la pantalla puede mostrar.
 *
 * El backend devuelve el código en el perfil `dev` y este `FakeEmailSender` solo alimenta
 * `demoInbox` en `AppContainer`; en `prod` el código se enviaría por correo real
 * (OPEN-QUESTIONS Q5).
 */
class FakeEmailSender : EmailSender {
    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    override suspend fun sendResetCode(toEmail: String, code: String) {
        _lastMessage.value = "Para $toEmail: tu código EDUVIA es $code. Vence en 10 minutos."
    }

    fun clear() { _lastMessage.value = null }
}