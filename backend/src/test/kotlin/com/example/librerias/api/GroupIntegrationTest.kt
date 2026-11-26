package com.example.librerias.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integración HTTP de grupos y alumnos (operaciones G1 a G7 de `docs/backend/api-contract.md`).
 *
 * Cubre la capa web, de servicio y SQL real: riesgo de que el código `XX-YYYY` no se genere o no se
 * busque sin distinguir mayúsculas (regla 2), de que la paginación de `PageResponse` esté mal o de
 * que vincular un alumno no actualice `studentCount` ni `parentId`. Verifica también el mapeo de
 * `parentId = 0` de la app frente a `NULL` en la BD y el 404 del código de clase inexistente.
 */
class GroupIntegrationTest : IntegrationTestBase() {

    private fun teacher(password: String = "Password1!"): Long =
        TestData(jdbc).user(passwordHash = PasswordEncoderForTest.encodeForTest(password))

    private fun auth(password: String = "Password1!") =
        authHeaders(tokenFor(password = password))

    @Test
    fun `crear grupo devuelve 201 con codigo unico`() {
        val id = teacher()
        val response = rest.exchange(
            "/api/v1/groups",
            HttpMethod.POST,
            HttpEntity(
                mapOf("teacherId" to id, "groupName" to "3º A", "schoolName" to "Colegio", "subject" to "Mate"),
                HttpHeaders().apply { addAll(auth()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        assertTrue((body["code"] as String).matches(Regex("[A-Z0-9]{2}-[A-Z0-9]{4}")))
        assertEquals(0, body["studentCount"])
        assertEquals(1, jdbc.queryForObject("select count(*) from school_groups", Int::class.java))
    }

    @Test
    fun `listar grupos por docente pagina correctamente`() {
        val id = teacher()
        val data = TestData(jdbc)
        data.group(id, "A", "AA-0001")
        data.group(id, "B", "BB-0002")
        val response = rest.exchange(
            "/api/v1/groups?teacherId=$id&page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(auth()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals(1, (body["items"] as List<*>).size)
        assertEquals(2L, (body["totalElements"] as Number).toLong())
        assertEquals(2, body["totalPages"])
    }

    @Test
    fun `buscar grupo por codigo ignora mayusculas`() {
        val id = teacher()
        TestData(jdbc).group(id, "A", "AB-1234")
        val response = rest.exchange(
            "/api/v1/groups/by-code?code=ab-1234",
            HttpMethod.GET,
            HttpEntity<Void>(auth()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("A", (response.body as Map<*, *>)["name"])
    }

    @Test
    fun `grupo inexistente devuelve 404`() {
        teacher()
        val response = rest.exchange(
            "/api/v1/groups/999",
            HttpMethod.GET,
            HttpEntity<Void>(auth()),
            Map::class.java,
        )
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, (response.body as Map<*, *>)["status"])
    }

    @Test
    fun `vincular alumno incrementa studentCount y mapea parentId`() {
        val id = teacher()
        TestData(jdbc).group(id, "A", "AB-1234")
        val parentId = teacher()
        val response = rest.exchange(
            "/api/v1/students:link",
            HttpMethod.POST,
            HttpEntity(
                mapOf("parentId" to parentId, "classCode" to "ab-1234", "studentName" to "Ana"),
                HttpHeaders().apply { addAll(auth()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Ana", body["name"])
        assertEquals(parentId, (body["parentId"] as Number).toLong())
        assertEquals(1, jdbc.queryForObject("select student_count from school_groups where code = 'AB-1234'", Int::class.java))
    }

    @Test
    fun `vincular alumno con codigo inexistente devuelve 404`() {
        val id = teacher()
        val response = rest.exchange(
            "/api/v1/students:link",
            HttpMethod.POST,
            HttpEntity(
                mapOf("parentId" to id, "classCode" to "ZZ-9999", "studentName" to "Ana"),
                HttpHeaders().apply { addAll(auth()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `listar alumnos de grupo y de padre`() {
        val id = teacher()
        val groupId = TestData(jdbc).group(id, "A", "AB-1234")
        TestData(jdbc).student("Ana", groupId, id)
        TestData(jdbc).student("Luis", groupId, null)

        val byGroup = rest.exchange(
            "/api/v1/groups/$groupId/students",
            HttpMethod.GET,
            HttpEntity<Void>(auth()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, byGroup.statusCode)
        val groupItems = (byGroup.body as Map<*, *>)["items"] as List<Map<*, *>>
        assertEquals(2, groupItems.size)
        // La app representa "sin tutor" con parentId = 0 (la BD guarda NULL).
        assertTrue(groupItems.any { (it["parentId"] as Number).toLong() == 0L })

        val byParent = rest.exchange(
            "/api/v1/students?parentId=$id",
            HttpMethod.GET,
            HttpEntity<Void>(auth()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, byParent.statusCode)
        val items = (byParent.body as Map<*, *>)["items"] as List<Map<*, *>>
        assertEquals(1, items.size)
        assertEquals(id, (items[0]["parentId"] as Number).toLong())
    }
}
