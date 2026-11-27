package com.example.librerias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppErrorKind
import com.example.librerias.model.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Gestiona el acceso con `LoginUseCase` (`container.login`) y el bloqueo tras intentos fallidos.
 * Expone a la UI `isLoading`, `error` y `lockSeconds`.
 */
class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Segundos que faltan para poder intentar otra vez (0 = sin bloqueo). */
    private val _lockSeconds = MutableStateFlow(0)
    val lockSeconds: StateFlow<Int> = _lockSeconds

    private var failedAttempts = 0

    fun clearError() { _error.value = null }

    fun login(email: String, password: String) {
        if (_isLoading.value || _lockSeconds.value > 0) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (val r = container.login(email, password)) {
                    // Si sale bien, la sesión cambia y AppNavHost abre la pantalla principal solo.
                    is AppResult.Success -> failedAttempts = 0
                    is AppResult.Error -> {
                        _error.value = r.message
                        // Solo cuentan las credenciales incorrectas: un fallo de red no debe
                        // bloquear al usuario ni agotar sus intentos.
                        if (r.error?.kind == AppErrorKind.CREDENTIALS ||
                            r.message.startsWith("Correo o contraseña")
                        ) {
                            failedAttempts++
                            if (failedAttempts >= 5) startLock()
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = container.errorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startLock() {
        failedAttempts = 0
        _error.value = "Demasiados intentos fallidos. Espera 30 segundos o recupera tu contraseña."
        viewModelScope.launch {
            for (sec in 30 downTo 1) {
                _lockSeconds.value = sec
                delay(1000)
            }
            _lockSeconds.value = 0
        }
    }
}