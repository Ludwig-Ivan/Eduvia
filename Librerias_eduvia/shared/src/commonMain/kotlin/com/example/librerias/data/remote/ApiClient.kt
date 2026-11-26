package com.example.librerias.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Crea el [HttpClient] con el motor nativo de cada plataforma (OkHttp en Android, Darwin en iOS).
 * Declarado `expect` en commonMain e implementado con `actual` en androidMain e iosMain para que la
 * configuración de red (timeouts, logging, serialización) sea idéntica en todas las plataformas.
 */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * URL base de la API resuelta por cada plataforma (`expect`/`actual`).
 * En Android se usa `10.0.2.2`, la IP con la que el emulador alcanza el servidor del equipo host;
 * en iOS se usa `localhost`, compartido con el simulador y permitido por ATS para red local.
 */
expect fun apiBaseUrl(): String

/**
 * Error de la capa de red. [status] es el código HTTP, [message] el texto listo para mostrar al
 * usuario y [problem] el `ProblemDetail` (RFC 9457) devuelto por el backend cuando viene en el cuerpo.
 *
 * Se construye manualmente en [ApiClient.toApiException] porque el cliente HTTP trabaja con
 * `expectSuccess = false`: los estados no 2xx no se lanzan solos y hay que convertirlos aquí.
 */
class ApiException(
    val status: Int,
    override val message: String,
    val problem: ProblemDto? = null,
) : RuntimeException(message)

/**
 * Capa de red de la app: serialización de DTOs, cabecera Bearer, renovación de tokens, reintento
 * tras un 401 y traducción de errores HTTP a [ApiException]. Los repositorios remotos construyen
 * sus llamadas sobre este cliente.
 *
 * @param baseUrl raíz de la API (por defecto, la de la plataforma); las rutas se resuelven relativas a ella.
 * @param session sesión en memoria de la que se lee el token para cada petición.
 * @param engine motor HTTP opcional para inyectar uno falso en pruebas.
 * @param engineConfig ajustes adicionales del motor, aplicados después de la configuración común.
 */
class ApiClient(
    baseUrl: String = apiBaseUrl(),
    val session: SessionStore,
    engine: HttpClientEngine? = null,
    engineConfig: HttpClientConfig<*>.() -> Unit = {},
) {
    /**
     * Configuración JSON compartida por `ContentNegotiation`:
     * - `ignoreUnknownKeys = true`: tolera campos de control del backend (auditoría, metadatos, etc.)
     *   que no están declarados en los DTOs de la app.
     * - `explicitNulls = false`: al serializar se omiten los campos nulos en lugar de enviar nulos
     *   explícitos, dejando que el backend aplique sus valores por defecto.
     * - `encodeDefaults = true`: envía los valores por defecto del contrato (`acceptedTerms = true`,
     *   `reminder = true`, listas vacías, etc.) aunque la llamada no los indique.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClientFactory.create(baseUrl, json, session, engine, engineConfig)

    /** GET que deserializa la respuesta como [T] mediante [request]. */
    suspend inline fun <reified T> get(path: String): T = request(HttpMethod.Get, path)

    /** POST con cuerpo opcional que deserializa la respuesta como [T] mediante [request]. */
    suspend inline fun <reified T> post(path: String, body: Any? = null): T =
        request(HttpMethod.Post, path, body)

    /** PUT con cuerpo opcional que deserializa la respuesta como [T] mediante [request]. */
    suspend inline fun <reified T> put(path: String, body: Any? = null): T =
        request(HttpMethod.Put, path, body)

    /** DELETE que deserializa la respuesta como [T] mediante [request]. */
    suspend inline fun <reified T> delete(path: String): T = request(HttpMethod.Delete, path)

    /**
     * Ejecuta la petición y deserializa el cuerpo como [T].
     *
     * Con `expectSuccess = false` Ktor no lanza ante un estado no 2xx: aquí se convierte la
     * respuesta a [ApiException] para poder leer el `ProblemDetail`. Si llega un 401 y hay refresh
     * token, se renueva la sesión con `auth/refresh` y la petición se reintenta una sola vez. La
     * exclusión de `auth/refresh` evita el bucle de reintentos; si la renovación falla, la sesión
     * se limpia y se propaga el error 401 original.
     */
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
    ): T {
        var response = execute(method, path, body)
        if (response.status.value == 401 && session.refreshToken.value != null && path != "auth/refresh") {
            if (refreshTokens()) response = execute(method, path, body)
        }
        if (!response.status.isSuccess()) throw response.toApiException()
        return response.body()
    }

    /**
     * POST que devuelve la respuesta HTTP sin deserializarla. Se usa cuando el cuerpo no interesa
     * o debe inspeccionarse antes de convertirlo (por ejemplo en los endpoints sin contenido).
     */
    suspend fun postRaw(path: String, body: Any? = null): HttpResponse = execute(HttpMethod.Post, path, body)

    /**
     * Para endpoints que devuelven 200/204 sin cuerpo (logout, reset-password).
     * No intenta deserializar: solo comprueba el estado y lanza [ApiException] si no es 2xx.
     */
    suspend fun postNoContent(path: String, body: Any? = null) {
        val response = postRaw(path, body)
        if (!response.status.isSuccess()) throw response.toApiException()
    }

    /**
     * Para endpoints DELETE que devuelven 204 sin cuerpo.
     * Igual que `postNoContent`, valida el estado y convierte el fallo a [ApiException].
     */
    suspend fun deleteNoContent(path: String) {
        val response = execute(HttpMethod.Delete, path)
        if (!response.status.isSuccess()) throw response.toApiException()
    }

    /**
     * Realiza la llamada HTTP resolviendo [path] respecto a la URL base. Añade la cabecera
     * `Authorization: Bearer <token>` si hay sesión activa y envía [body] como JSON cuando se
     * proporciona. Devuelve la respuesta sin lanzar por su código de estado.
     */
    suspend fun execute(method: HttpMethod, path: String, body: Any? = null): HttpResponse =
        client.request(path) {
            this.method = method
            session.token.value?.let { headers.append("Authorization", "Bearer $it") }
            body?.let { setBody(it) }
        }

    /**
     * Renueva access token y refresh token con `POST auth/refresh` (rotación de refresh). Usa el
     * cliente HTTP directamente, sin pasar por [request], para no volver a entrar en el reintento
     * por 401. Al tener éxito actualiza [SessionStore] (tokens y, si vienen, usuario); si falla o
     * no hay refresh token, limpia la sesión y devuelve `false`.
     */
    suspend fun refreshTokens(): Boolean {
        val refresh = session.refreshToken.value ?: return false
        return try {
            val fresh: TokenResponseDto = client.request("auth/refresh") {
                method = HttpMethod.Post
                setBody(RefreshRequestDto(refresh))
            }.body()
            session.setTokens(fresh.accessToken, fresh.refreshToken)
            fresh.user?.let { session.setUser(it.toModel()) }
            true
        } catch (_: Exception) {
            session.clear()
            false
        }
    }

    /**
     * Convierte una respuesta no 2xx en [ApiException]. Intenta leer el cuerpo como `ProblemDto`
     * (RFC 9457) y elige el mensaje en este orden: `detail`, mensaje del primer error de campo,
     * `title`, texto en crudo o un `Error <status>` genérico.
     */
    @PublishedApi
    internal suspend fun HttpResponse.toApiException(): ApiException {
        val text = bodyAsText()
        val problem = runCatching { json.decodeFromString<ProblemDto>(text) }.getOrNull()
        val message = problem?.detail
            ?: problem?.errors?.firstOrNull()?.message
            ?: problem?.title
            ?: text.takeIf { it.isNotBlank() }
            ?: "Error ${status.value}"
        return ApiException(status.value, message, problem)
    }

    /**
     * Texto de error para la UI: usa el mensaje de [ApiException], sustituye los fallos de
     * serialización por «Respuesta inesperada del servidor» y, si no hay mensaje, indica que no se
     * pudo conectar con el servidor.
     */
    fun errorMessage(error: Throwable): String = when (error) {
        is ApiException -> error.message
        is SerializationException -> "Respuesta inesperada del servidor"
        else -> error.message ?: "No se pudo conectar con el servidor"
    }
}

/**
 * Fábrica internal para poder inyectar un engine en pruebas sin exponer el HttpClient.
 *
 * Configura el cliente común: `ContentNegotiation` con el [Json] de [ApiClient], `HttpTimeout`
 * (15 s de petición y 10 s de conexión), `Logging` en nivel INFO y `defaultRequest` con la URL
 * base y `Content-Type: application/json`. `engineConfig` se aplica al final para permitir
 * ajustes propios de cada plataforma o de los tests.
 */
internal object HttpClientFactory {
    fun create(
        baseUrl: String,
        json: Json,
        session: SessionStore,
        engine: HttpClientEngine?,
        engineConfig: HttpClientConfig<*>.() -> Unit,
    ): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            // Nosotros convertimos los status no-2xx a ApiException para leer el ProblemDetail.
            expectSuccess = false
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            install(Logging) { level = LogLevel.INFO }
            engineConfig()
        }
        return if (engine != null) HttpClient(engine, config) else createPlatformHttpClient(config)
    }
}
