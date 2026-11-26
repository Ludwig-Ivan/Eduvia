package com.example.librerias.api.web

import com.example.librerias.api.service.AuthService
import com.example.librerias.api.web.dto.ExistsResponse
import com.example.librerias.api.web.dto.LoginRequest
import com.example.librerias.api.web.dto.RefreshRequest
import com.example.librerias.api.web.dto.ResetCodeRequest
import com.example.librerias.api.web.dto.ResetCodeResponse
import com.example.librerias.api.web.dto.ResetPasswordRequest
import com.example.librerias.api.web.dto.RegisterRequest
import com.example.librerias.api.web.dto.toDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Capa controller del contrato de autenticación y cuenta (A1-A9 en `docs/backend/api-contract.md`).
 *
 * Colabora exclusivamente con [AuthService]: este controlador traduce las peticiones HTTP de la app KMP en llamadas al servicio y deja que el servicio resuelva la lógica de tokens, duplicidades y recuperación de contraseña. Las operaciones de autenticación bajo `/api/v1/auth` son públicas; [me] consume el `Jwt` que inyecta el filtro de seguridad. Los fallos de dominio y de validación se convierten en `ProblemDetail` (RFC 9457) en [GlobalExceptionHandler]; para las rutas protegidas, la cadena de seguridad responde 401 si falta un Bearer válido y 403 si hay una denegación por permisos antes de llegar al servicio.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Autenticación",
    description = "Registro, login, renovación y cierre de sesión, comprobación de email/teléfono y recuperación de contraseña del contrato con la app KMP.",
)
class AuthController(private val service: AuthService) {

    /**
     * Registra una cuenta y delega en [AuthService] la transacción atómica que crea los grupos del docente o vincula los alumnos del padre.
     *
     * Capa controller: `@Valid` aplica las restricciones del DTO antes de invocar al servicio; la respuesta 201 entrega en una sola llamada el usuario, los grupos creados y los tokens que la app KMP necesita para abrir la sesión.
     *
     * @param body solicitud de registro validada; incluye las listas `children` y `groups` que el servicio interpreta según el rol.
     * @return `201 Created` con el `TokenResponse` del contrato A1.
     * @throws ApiException con 400 si las contraseñas no coinciden, faltan los términos, el teléfono es inválido o un código de clase no existe; con 409 si el email, el teléfono o el código de grupo están duplicados. [GlobalExceptionHandler] lo convierte en `ProblemDetail` (RFC 9457).
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/register")
    @Operation(
        summary = "Registrar una cuenta",
        description = "Crea usuario y datos asociados en una sola transacción; devuelve los tokens de acceso para que la app KMP inicie sesión sin una segunda petición.",
    )
    @ApiResponse(responseCode = "201", description = "Cuenta creada con usuario, grupos y tokens.")
    @ApiResponse(responseCode = "400", description = "Validación incorrecta o código de clase inexistente.")
    @ApiResponse(responseCode = "409", description = "Email, teléfono o código de grupo duplicado.")
    fun register(@Valid @RequestBody body: RegisterRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(service.register(body))

    /**
     * Autentica al usuario delegando la comparación de credenciales y la emisión de tokens en [AuthService].
     *
     * Capa controller: `@Valid` garantiza que email y contraseña tengan el formato esperado por la app KMP antes de consultar al servicio; es una operación pública.
     *
     * @param body credenciales validadas de inicio de sesión.
     * @return `200 OK` con el `TokenResponse` del contrato A2.
     * @throws ApiException con 401 si el email o la contraseña no son válidos; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/login")
    @Operation(
        summary = "Iniciar sesión",
        description = "Valida email y contraseña y devuelve access token, refresh token y usuario para la sesión de la app KMP.",
    )
    @ApiResponse(responseCode = "200", description = "Credenciales correctas; tokens emitidos.")
    @ApiResponse(responseCode = "400", description = "Email inválido o contraseña vacía.")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas.")
    fun login(@Valid @RequestBody body: LoginRequest) = service.login(body)

    /**
     * Renueva el par de tokens cuando el access token caduca, usando el refresh token vigente.
     *
     * Capa controller: solo valida el DTO y delega en [AuthService] la rotación del refresh token, que impide reutilizarlo. Es pública porque el access token ya no sirve para renovarse.
     *
     * @param body refresh token validado enviado por la app KMP.
     * @return `200 OK` con un nuevo `TokenResponse` (contrato A3).
     * @throws ApiException con 401 si el refresh token es desconocido, está revocado, expiró o el usuario ya no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/refresh")
    @Operation(
        summary = "Renovar tokens",
        description = "Rota el refresh token y emite un nuevo access token cuando la sesión de la app KMP ha caducado.",
    )
    @ApiResponse(responseCode = "200", description = "Nuevos tokens emitidos.")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, revocado o expirado.")
    fun refresh(@Valid @RequestBody body: RefreshRequest) = service.refresh(body)

    /**
     * Cierra la sesión revocando el refresh token, sin invalidar access tokens ya emitidos.
     *
     * Capa controller: valida el cuerpo y delega la revocación en [AuthService]; responde sin contenido porque la app KMP solo necesita confirmar que la operación terminó.
     *
     * @param body refresh token validado que se debe revocar.
     * @return `204 No Content` (contrato A4).
     * @throws ApiException con 401 si el refresh token es desconocido o ya estaba revocado; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/logout")
    @Operation(
        summary = "Cerrar sesión",
        description = "Revoca el refresh token para que la app KMP no pueda renovar la sesión; la respuesta no incluye cuerpo.",
    )
    @ApiResponse(responseCode = "204", description = "Refresh token revocado.")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o ya revocado.")
    fun logout(@Valid @RequestBody body: RefreshRequest): ResponseEntity<Void> {
        service.logout(body)
        return ResponseEntity.noContent().build()
    }

    /**
     * Comprueba si un email ya está registrado, para que la app KMP valide el campo antes de registrarse.
     *
     * Capa controller: publica directamente el booleano calculado por [AuthService], que normaliza el email a minúsculas como exige la regla de duplicidad del contrato.
     *
     * @param email email a comprobar, recibido como parámetro de consulta.
     * @return `200 OK` con [ExistsResponse]; `true` significa que el email ya existe.
     * @throws Exception si el enlazado de la query falla (400) o si ocurre un error no controlado (500).
     */
    @GetMapping("/auth/exists/email")
    @Operation(
        summary = "Comprobar email",
        description = "Consulta de existencia usada por la validación en tiempo de registro de la app KMP; la búsqueda ignora mayúsculas.",
    )
    @ApiResponse(responseCode = "200", description = "Resultado de la comprobación.")
    @ApiResponse(responseCode = "400", description = "Parámetro de consulta no válido.")
    fun emailExists(
        @Parameter(description = "Email a comprobar; se normaliza a minúsculas antes de consultar.", example = "ana@example.com")
        @RequestParam email: String,
    ) = ExistsResponse(service.emailExists(email))

    /**
     * Comprueba si un teléfono ya está registrado, usando la misma normalización a dígitos que el registro.
     *
     * Capa controller: delega la consulta en [AuthService] y devuelve el booleano que la app KMP muestra durante el alta.
     *
     * @param phone teléfono a comprobar, recibido como parámetro de consulta.
     * @return `200 OK` con [ExistsResponse]; `true` significa que el teléfono ya existe.
     * @throws Exception si el enlazado de la query falla (400) o si ocurre un error no controlado (500).
     */
    @GetMapping("/auth/exists/phone")
    @Operation(
        summary = "Comprobar teléfono",
        description = "Consulta de existencia para el registro; el servicio conserva solo los dígitos del valor recibido.",
    )
    @ApiResponse(responseCode = "200", description = "Resultado de la comprobación.")
    @ApiResponse(responseCode = "400", description = "Parámetro de consulta no válido.")
    fun phoneExists(
        @Parameter(description = "Teléfono a comprobar; se conservan solo sus dígitos.", example = "612345678")
        @RequestParam phone: String,
    ) = ExistsResponse(service.phoneExists(phone))

    /**
     * Solicita un código de restablecimiento y delega en [AuthService] su generación, caducidad y envío simulado.
     *
     * Capa controller: responde siempre 200, incluso si el email no existe, para no revelar qué cuentas están registradas; en `dev` el servicio puede incluir el código para el `demoInbox` de la app KMP.
     *
     * @param body email validado al que se envía el código.
     * @return `200 OK` con [ResetCodeResponse] (código presente solo en el perfil `dev`).
     * @throws ApiException con 400 si el correo no supera las comprobaciones del servicio; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/reset-code")
    @Operation(
        summary = "Solicitar código de restablecimiento",
        description = "Genera un código de seis dígitos con caducidad y un número limitado de intentos; la respuesta no revela si la cuenta existe.",
    )
    @ApiResponse(responseCode = "200", description = "Solicitud aceptada; el código solo se expone en el perfil dev.")
    @ApiResponse(responseCode = "400", description = "Email no válido.")
    fun resetCode(@Valid @RequestBody body: ResetCodeRequest) =
        ResetCodeResponse(service.requestResetCode(body.email))

    /**
     * Cambia la contraseña usando un código de restablecimiento válido, delegando la verificación en [AuthService].
     *
     * Capa controller: `@Valid` comprueba el DTO y el servicio aplica las reglas de los cinco intentos, los diez minutos y un solo uso; responde sin cuerpo porque la app KMP solo necesita saber que el cambio terminó.
     *
     * @param body email, código y nueva contraseña (con su confirmación) validados.
     * @return `200 OK` sin contenido (contrato A8).
     * @throws ApiException con 400 si las contraseñas no coinciden, el código es incorrecto, está vencido o agotado; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/auth/reset-password")
    @Operation(
        summary = "Restablecer contraseña",
        description = "Aplica la nueva contraseña si el código sigue vigente y consume el código para impedir su reutilización.",
    )
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada.")
    @ApiResponse(responseCode = "400", description = "Contraseñas distintas o código incorrecto, vencido o agotado.")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequest): ResponseEntity<Void> {
        service.resetPassword(body.email, body.code, body.newPassword, body.confirmPassword)
        return ResponseEntity.ok().build()
    }

    /**
     * Devuelve el usuario asociado al access token, para que la app KMP recupere la sesión al arrancar.
     *
     * Capa controller: extrae el `subject` del [Jwt] inyectado por el filtro de seguridad y delega la búsqueda en [AuthService]; el usuario se serializa con el DTO compartido con el contrato A9.
     *
     * @param jwt token Bearer validado por la capa de seguridad; su `subject` es el id del usuario.
     * @return `200 OK` con el `UserDto` de la sesión actual.
     * @throws ApiException con 404 si el usuario del `subject` ya no existe; se convierte en `ProblemDetail` con ese código.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Usuario actual",
        description = "Recupera el usuario del JWT de la sesión para que la app KMP restaure su estado sin volver a iniciar sesión.",
    )
    @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun me(@AuthenticationPrincipal jwt: Jwt) =
        service.currentUser(jwt.subject!!.toLong()).toDto()
}
