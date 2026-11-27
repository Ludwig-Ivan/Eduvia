package com.example.librerias.domain.repository

import com.example.librerias.model.AppResult
import com.example.librerias.model.User
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.StateFlow

/**
 * Capa de dominio: contrato de autenticación, registro y sesión, independiente del origen de datos.
 *
 * Lo satisface `RemoteAuthRepository` (HTTP con `SessionStore`). Coincide con el contrato de API
 * `auth/login`, `auth/register`, `auth/exists/email`, `auth/exists/phone`, `auth/reset-code`,
 * `auth/reset-password` y `auth/logout`; los casos de uso solo conocen esta interfaz.
 */
interface AuthRepository {
    /** Usuario con sesión activa (null = sin sesión). La sesión es solo en memoria: no sobrevive al proceso. */
    val currentUser: StateFlow<User?>

    /**
     * Autentica con [email] y [password].
     * @return el usuario con la sesión iniciada, o `AppResult.Error` si las credenciales fallan.
     */
    suspend fun login(email: String, password: String): AppResult<User>

    /**
     * Crea una cuenta con [role], [fullName], [email], [phone] y [password].
     * El repositorio vuelve a comprobar correo y celular repetidos como última barrera.
     * @return el usuario creado con la sesión iniciada, o `AppResult.Error` si ya existe la cuenta.
     */
    suspend fun register(
        role: UserRole,
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): AppResult<User>

    /**
     * Consulta si [email] ya pertenece a una cuenta.
     * @return `true` si el correo está en uso, o `AppResult.Error` si no se pudo consultar.
     */
    suspend fun emailExists(email: String): AppResult<Boolean>

    /**
     * Consulta si [phone] ya pertenece a una cuenta.
     * @return `true` si el celular está en uso, o `AppResult.Error` si no se pudo consultar.
     */
    suspend fun phoneExists(phone: String): AppResult<Boolean>

    /**
     * Genera y guarda un código de recuperación. Devuelve null si el correo no existe.
     * En la demo el código vence en 10 minutos, admite 5 intentos y uno nuevo reemplaza al anterior.
     * @return el código de 6 dígitos para [email], null si no tiene cuenta, o `AppResult.Error`
     * si la API no respondió (para no mostrar un envío falso).
     */
    suspend fun createResetCode(email: String): AppResult<String?>

    /**
     * Cambia la contraseña de [email] si [code] sigue siendo válido.
     * @return `AppResult.Success` al aplicar [newPassword], o `AppResult.Error` si el código
     * es incorrecto, venció o agotó los intentos; el código solo sirve una vez.
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): AppResult<Unit>

    /**
     * Cierra la sesión: limpia [currentUser] y, en la implementación HTTP, revoca el refresh token.
     * No devuelve resultado.
     */
    fun logout()
}
