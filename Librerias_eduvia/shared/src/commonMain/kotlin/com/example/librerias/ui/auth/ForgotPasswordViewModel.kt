package com.example.librerias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotUiState(
    val isSending: Boolean = false,
    val isResetting: Boolean = false,
    val cooldown: Int = 0,          // segundos para poder reenviar
    val info: String? = null,
    val error: String? = null,
    val done: Boolean = false
)

/**
 * Gestiona la recuperación de contraseña con `SendResetCodeUseCase` y `ResetPasswordUseCase`.
 * Expone `state` (envío, reintento, error y resultado) y `demoInbox`, el correo de demostración.
 */
class ForgotPasswordViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ForgotUiState())
    val state: StateFlow<ForgotUiState> = _state.asStateFlow()

    /** Solo demo: el "correo" que mandaría el servidor. */
    val demoInbox: StateFlow<String?> = container.demoInbox

    /** Al abrir la pantalla, limpia restos de una visita anterior. */
    fun reset() {
        container.clearDemoInbox()
        _state.update { it.copy(done = false, error = null, info = null, isResetting = false, isSending = false) }
    }

    fun sendCode(email: String) {
        val s = _state.value
        if (s.isSending || s.cooldown > 0) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null, info = null) }
            container.clearDemoInbox()
            try {
                when (val r = container.sendResetCode(email)) {
                    is AppResult.Success -> {
                        _state.update {
                            it.copy(
                                isSending = false,
                                info = "Si el correo está registrado, te enviamos un código. Vence en 10 minutos."
                            )
                        }
                        startCooldown()
                    }
                    is AppResult.Error -> _state.update { it.copy(isSending = false, error = r.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false, error = container.errorMessage(e)) }
            }
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _state.update { it.copy(cooldown = sec) }
                delay(1000)
            }
            _state.update { it.copy(cooldown = 0) }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        if (_state.value.isResetting) return
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true, error = null) }
            try {
                when (val r = container.resetPassword(email, code.trim(), newPassword, confirmPassword)) {
                    is AppResult.Success -> _state.update { it.copy(isResetting = false, done = true, info = null) }
                    is AppResult.Error -> _state.update { it.copy(isResetting = false, error = r.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isResetting = false, error = container.errorMessage(e)) }
            }
        }
    }
}