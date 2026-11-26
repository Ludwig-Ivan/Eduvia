package com.example.librerias.data.remote

import com.example.librerias.domain.repository.AuthRepository
import com.example.librerias.model.AppResult
import com.example.librerias.model.User
import com.example.librerias.model.UserRole
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Implementación del contrato de dominio [AuthRepository] sobre HTTP: consume los endpoints de
 * autenticación (A1, A2, A4-A8; A3 lo renueva [ApiClient]) y mantiene [SessionStore] sincronizado
 * con el usuario y los tokens. No usa caché de listados;
 * los errores de red o del backend se devuelven como `AppResult.Error` con el mensaje de
 * [ApiClient.errorMessage] (las comprobaciones de existencia devuelven `false`).
 */
class RemoteAuthRepository(
    private val api: ApiClient,
    private val session: SessionStore,
) : AuthRepository {

    private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Usuario de la sesión en memoria; la UI lo observa sin nuevos viajes de red. */
    override val currentUser: StateFlow<User?> = session.currentUser

    /**
     * `POST auth/login` (A2): envía [LoginRequestDto], mapea `user` a dominio con `toModel` y guarda
     * tokens y usuario en la sesión. Si el servidor no devuelve usuario o hay error, propaga el
     * mensaje como `AppResult.Error`.
     */
    override suspend fun login(email: String, password: String): AppResult<User> = try {
        val response: TokenResponseDto = api.post("auth/login", LoginRequestDto(email, password))
        val user = response.user?.toModel()
            ?: return AppResult.Error("El servidor no devolvió el usuario.")
        session.setTokens(response.accessToken, response.refreshToken)
        session.setUser(user)
        AppResult.Success(user)
    } catch (e: Exception) {
        AppResult.Error(api.errorMessage(e))
    }

    /**
     * `POST auth/register` (A1): envía [RegisterRequestDto] con el rol como literal y la contraseña
     * confirmada; mapea el usuario devuelto y lo guarda junto a los tokens en la sesión. No envía
     * hijos ni grupos en esta llamada. Errores → `AppResult.Error`.
     */
    override suspend fun register(
        role: UserRole,
        fullName: String,
        email: String,
        phone: String,
        password: String,
    ): AppResult<User> = try {
        val response: TokenResponseDto = api.post(
            "auth/register",
            RegisterRequestDto(
                role = role.name,
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                confirmPassword = password,
                acceptedTerms = true,
            ),
        )
        val user = response.user?.toModel()
            ?: return AppResult.Error("El servidor no devolvió el usuario.")
        session.setTokens(response.accessToken, response.refreshToken)
        session.setUser(user)
        AppResult.Success(user)
    } catch (e: Exception) {
        AppResult.Error(api.errorMessage(e))
    }

    /**
     * `GET auth/exists/email` (A5): codifica el email como parámetro de query y lee [ExistsDto].
     * Sin caché; un error de API (`ApiException`) se interpreta como `false`.
     */
    override suspend fun emailExists(email: String): Boolean = try {
        api.get<ExistsDto>("auth/exists/email?email=${email.encodeURLParameter()}").exists
    } catch (e: ApiException) {
        false
    }

    /**
     * `GET auth/exists/phone` (A6): codifica el teléfono como parámetro de query y lee [ExistsDto].
     * Sin caché; un error de API (`ApiException`) se interpreta como `false`.
     */
    override suspend fun phoneExists(phone: String): Boolean = try {
        api.get<ExistsDto>("auth/exists/phone?phone=${phone.encodeURLParameter()}").exists
    } catch (e: ApiException) {
        false
    }

    /**
     * `POST auth/reset-code` (A7): devuelve el `resetCode` que el backend incluye solo en `dev` para
     * el buzón de demostración. Sin caché; cualquier excepción se traduce a `null`.
     */
    override suspend fun createResetCode(email: String): String? = try {
        api.post<ResetCodeDto>("auth/reset-code", ResetCodeRequestDto(email)).resetCode
    } catch (e: Exception) {
        null
    }

    /**
     * `POST auth/reset-password` (A8): usa `postNoContent` porque la respuesta no tiene cuerpo; al
     * tener éxito limpia la sesión en memoria. Errores → `AppResult.Error` con mensaje de la API.
     */
    override suspend fun resetPassword(email: String, code: String, newPassword: String): AppResult<Unit> = try {
        api.postNoContent(
            "auth/reset-password",
            ResetPasswordRequestDto(email, code, newPassword, newPassword),
        )
        session.clear()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Error(api.errorMessage(e))
    }

    /**
     * Cierra la sesión local de inmediato y lanza `POST auth/logout` (A4) en segundo plano con el
     * refresh token para revocarlo en el servidor. El fallo de red se ignora (`runCatching`); no hay
     * caché de listados que limpiar aquí.
     */
    override fun logout() {
        val refresh = session.refreshToken.value
        session.clear()
        if (refresh != null) {
            logoutScope.launch {
                runCatching { api.postNoContent("auth/logout", RefreshRequestDto(refresh)) }
            }
        }
    }
}
