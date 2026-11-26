package com.example.librerias.data.remote

import com.example.librerias.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de sesión compartido entre el cliente HTTP y los repositorios.
 *
 * La sesión es solo en memoria (`MutableStateFlow`): no se persiste en disco, así que al reiniciar
 * el proceso hay que iniciar sesión de nuevo. `currentUser` se expone como [StateFlow] para que la
 * UI reaccione al instante a login/logout, mientras el cliente HTTP lee [token] y [refreshToken]
 * en cada petición.
 */
class SessionStore {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** Guarda el access token (Bearer) y el refresh token tras login, registro o renovación. */
    fun setTokens(accessToken: String, refreshToken: String) {
        _token.value = accessToken
        _refreshToken.value = refreshToken
    }

    /** Actualiza el usuario observado por la UI; `null` indica que no hay sesión iniciada. */
    fun setUser(user: User?) {
        _currentUser.value = user
    }

    /** Cierra la sesión en memoria: borra ambos tokens y el usuario (logout o refresh fallido). */
    fun clear() {
        _token.value = null
        _refreshToken.value = null
        _currentUser.value = null
    }
}
