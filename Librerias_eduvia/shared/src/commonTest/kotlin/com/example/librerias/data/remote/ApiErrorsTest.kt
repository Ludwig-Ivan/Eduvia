package com.example.librerias.data.remote

import com.example.librerias.model.AppErrorKind
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Traducción de fallos de red y del backend a mensajes seguros para la UI. */
class ApiErrorsTest {

    @Test
    fun error_de_red_se_vuelve_en_mensaje_amigable_sin_url_ni_texto_de_ktor() = runBlocking {
        val api = testApi { throw IOException("unexpected end of stream on http://10.0.2.2:8080/auth/login") }

        val error = assertFailsWith<IOException> { api.get<GroupDto>("auth/login") }
        val appError = api.errorOf(error)

        assertEquals(AppErrorKind.CONNECTION, appError.kind)
        assertTrue(appError.retryable)
        assertEquals("No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.", appError.message)
        assertFalse(appError.message.contains("http://"))
        assertFalse(appError.message.contains("unexpected end"))
    }

    @Test
    fun timeout_se_clasifica_como_reintentable() {
        val api = testApi { jsonResponse(GROUP_JSON) }
        val error = api.errorOf(IOException("Socket timeout has expired [url=http://10.0.2.2:8080/api/v1/auth/login]"))

        assertEquals(AppErrorKind.TIMEOUT, error.kind)
        assertTrue(error.retryable)
        assertFalse(error.message.contains("Socket timeout"))
    }

    @Test
    fun error_5xx_del_servidor_no_expone_el_cuerpo_tecnico() = runBlocking {
        val api = testApi { jsonResponse("Internal Server Error: java.lang.NullPointerException", HttpStatusCode.InternalServerError) }

        val error = assertFailsWith<ApiException> { api.get<GroupDto>("tasks") }
        val appError = api.errorOf(error)

        assertEquals(AppErrorKind.SERVER, appError.kind)
        assertTrue(appError.retryable)
        assertFalse(appError.message.contains("NullPointerException"))
        assertFalse(appError.message.contains("java."))
    }

    @Test
    fun un_401_en_login_es_de_credenciales_y_un_401_en_otra_ruta_es_de_sesion() = runBlocking {
        val api = testApi {
            jsonResponse(
                """{"title":"Unauthorized","status":401,"detail":"Credenciales inválidas"}""",
                HttpStatusCode.Unauthorized,
            )
        }

        val loginError = assertFailsWith<ApiException> { api.get<GroupDto>("auth/login") }
        val otherError = assertFailsWith<ApiException> { api.get<GroupDto>("tasks") }

        assertEquals(AppErrorKind.CREDENTIALS, api.errorOf(loginError).kind)
        assertTrue(api.errorOf(loginError).message.startsWith("Correo o contraseña"))
        assertEquals(AppErrorKind.SESSION, api.errorOf(otherError).kind)
    }

    @Test
    fun un_404_con_detail_legible_se_muestra_y_un_404_tecnico_se_sustituye() = runBlocking {
        val friendly = testApi { jsonResponse(PROBLEM_JSON, HttpStatusCode.NotFound) }
        val technical = testApi {
            jsonResponse(
                """{"title":"Not Found","status":404,"detail":"http://internal/api/v1/groups/999"}""",
                HttpStatusCode.NotFound,
            )
        }

        val friendlyError = assertFailsWith<ApiException> { friendly.get<GroupDto>("groups/999") }
        val technicalError = assertFailsWith<ApiException> { technical.get<GroupDto>("groups/999") }

        assertEquals("Grupo no encontrado", friendly.errorMessage(friendlyError))
        assertEquals(
            "No encontramos lo que buscabas. Puede que ya no esté disponible.",
            technical.errorMessage(technicalError),
        )
    }
}
