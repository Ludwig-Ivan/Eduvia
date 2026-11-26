package com.example.librerias.data.remote

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/** Capa de cliente HTTP: protege rutas, token, query, paginación, `ProblemDetail`, renovación 401 y respuestas sin cuerpo; cubre desviaciones del contrato de red. */
class ApiClientTest {

    @Test
    fun get_envia_la_ruta_el_token_y_el_query_y_parsea_la_page() = runBlocking {
        var authorizedHeader: String? = null
        val api = testApi { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/groups", request.url.encodedPath)
            assertEquals("1", request.url.parameters["teacherId"])
            authorizedHeader = request.headers["Authorization"]
            jsonResponse(pageOf(GROUP_JSON, total = 10))
        }
        api.session.setTokens("access-1", "refresh-1")

        val page = api.get<PageDto<GroupDto>>("groups?teacherId=1")

        assertEquals("Bearer access-1", authorizedHeader)
        assertEquals(1, page.items.size)
        assertEquals("MA-4K7P", page.items.first().code)
        assertEquals(10L, page.totalElements)
    }

    @Test
    fun un_error_http_se_convierte_en_ApiException_con_el_ProblemDetail() = runBlocking {
        val api = testApi { jsonResponse(PROBLEM_JSON, HttpStatusCode.NotFound) }

        val error = assertFailsWith<ApiException> { api.get<GroupDto>("groups/999") }

        assertEquals(404, error.status)
        assertEquals("Grupo no encontrado", error.message)
        assertNotNull(error.problem)
        assertEquals("/api/v1/groups/999", error.problem.instance)
    }

    @Test
    fun un_401_renueva_el_token_y_reintenta_la_peticion() = runBlocking {
        var groupCalls = 0
        var refreshCalls = 0
        var usedTokenOnRetry: String? = null
        val api = testApi { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") -> {
                    refreshCalls++
                    jsonResponse(LOGIN_JSON.replace("access-1", "access-2"))
                }
                groupCalls++ == 0 -> jsonResponse(PROBLEM_JSON, HttpStatusCode.Unauthorized)
                else -> {
                    usedTokenOnRetry = request.headers["Authorization"]
                    jsonResponse(GROUP_JSON)
                }
            }
        }
        api.session.setTokens("access-1", "refresh-1")

        val group = api.get<GroupDto>("groups/1")

        assertEquals(1, refreshCalls)
        assertEquals(2, groupCalls)
        assertEquals("Bearer access-2", usedTokenOnRetry)
        assertEquals("access-2", api.session.token.value)
        assertEquals(1L, group.id)
    }

    @Test
    fun postNoContent_y_deleteNoContent_aceptan_respuestas_sin_cuerpo() = runBlocking {
        val api = testApi { request ->
            assertEquals(HttpMethod.Post, request.method)
            jsonResponse("", HttpStatusCode.NoContent)
        }

        api.postNoContent("auth/logout", RefreshRequestDto("refresh-1"))
    }

    @Test
    fun errorMessage_devuelve_el_detail_del_problem_y_no_el_texto_generico() = runBlocking {
        val api = testApi { jsonResponse(PROBLEM_JSON, HttpStatusCode.NotFound) }
        val error = assertFailsWith<ApiException> { api.get<GroupDto>("groups/999") }
        val asThrowable: Throwable = error

        assertEquals("Grupo no encontrado", api.errorMessage(asThrowable))
    }
}
