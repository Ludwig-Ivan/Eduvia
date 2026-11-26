package com.example.librerias.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integración HTTP de juntas (operaciones M1 a M9 de `docs/backend/api-contract.md`).
 *
 * Cubre la capa web, de servicio y SQL real: riesgo de que el evento de calendario no use
 * `1000 + meetings.id` (regla 4), de que se dupliquen juntas con la misma fecha, hora, lugar y grupo
 * (regla 5), de que `isNext`, `relativeLabel` o `AttendanceSummary` se calculen mal (regla 7), o de
 * que M9 no actualice el evento ya existente. También verifica los 400 de validación.
 */
class MeetingIntegrationTest : IntegrationTestBase() {

    private fun setup(): Pair<Long, Long> {
        val teacherId = TestData(jdbc).user(passwordHash = PasswordEncoderForTest.encodeForTest("Password1!"))
        val groupId = TestData(jdbc).group(teacherId, "A", "AB-1234", studentCount = 2)
        return teacherId to groupId
    }

    private fun headers() = authHeaders(tokenFor(password = "Password1!"))

    private fun createMeeting(groupId: Long, time: String = "8:00 am", place: String = "Aula 1") =
        rest.exchange(
            "/api/v1/meetings",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "title" to "Junta", "dateText" to "2026-12-10", "time" to time,
                    "place" to place, "link" to "https://meet.example.com/x", "reminder" to true,
                    "groupId" to groupId,
                ),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )

    @Test
    fun `crear junta crea evento de calendario con id 1000 mas el id`() {
        val (_, groupId) = setup()
        val response = createMeeting(groupId)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        val id = (body["id"] as Number).toLong()
        assertEquals("2026-12-10", body["date"])
        assertEquals(true, body["hasVirtualLink"])
        assertEquals("PENDING", body["response"])
        assertEquals(
            1,
            jdbc.queryForObject("select count(*) from calendar_events where id = ? and type = 'MEETING'", Int::class.java, 1000L + id),
        )
    }

    @Test
    fun `junta duplicada en fecha hora y lugar devuelve 409`() {
        val (_, groupId) = setup()
        assertEquals(HttpStatus.CREATED, createMeeting(groupId).statusCode)
        val duplicate = createMeeting(groupId, time = "8:00 AM", place = "aula 1")
        assertEquals(HttpStatus.CONFLICT, duplicate.statusCode)
    }

    @Test
    fun `listar juntas calcula isNext y relativeLabel`() {
        val (_, groupId) = setup()
        createMeeting(groupId)
        val response = rest.exchange(
            "/api/v1/meetings?page=0&size=10",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        val item = ((response.body as Map<*, *>)["items"] as List<Map<*, *>>)[0]
        assertEquals(true, item["isNext"])
        assertTrue(item["relativeLabel"] is String)
        assertEquals("8:00 am", item["time"])
    }

    // Un solo test cubre M6 (respuesta), M5 (asistencia por junta) y M7 (respuestas por alumno).
    @Test
    fun `set response y asistencia derivada`() {
        val (teacherId, groupId) = setup()
        val meetingId = ((createMeeting(groupId).body as Map<*, *>)["id"] as Number).toLong()
        val studentId = TestData(jdbc).student("Ana", groupId, teacherId)
        val response = rest.exchange(
            "/api/v1/meetings/$meetingId/responses/$studentId",
            HttpMethod.PUT,
            HttpEntity(mapOf("response" to "ATTENDING"), HttpHeaders().apply { addAll(headers()) }),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ATTENDING", (response.body as Map<*, *>)["response"])

        val attendance = rest.exchange(
            "/api/v1/meetings/$meetingId/attendance",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, attendance.statusCode)
        val items = (attendance.body as Map<*, *>)["items"] as List<Map<*, *>>
        assertEquals(1, items.size)
        assertEquals("Ana", items[0]["familyName"])
        assertEquals("ATTENDING", items[0]["response"])

        val byStudent = rest.exchange(
            "/api/v1/meetings/responses?studentId=$studentId",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, byStudent.statusCode)
        val map = ((byStudent.body as Map<*, *>)["responses"] as Map<*, *>)
        assertEquals("ATTENDING", map[meetingId.toString()])
    }

    @Test
    fun `recordatorios devuelven numero de pendientes`() {
        val (teacherId, groupId) = setup()
        val meetingId = ((createMeeting(groupId).body as Map<*, *>)["id"] as Number).toLong()
        TestData(jdbc).student("Ana", groupId, teacherId)
        // Sin respuestas, el conteo de PENDING explícitas es 0.
        val response = rest.exchange(
            "/api/v1/meetings/$meetingId/reminders",
            HttpMethod.POST,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, (response.body as Map<*, *>)["pending"])
    }

    @Test
    fun `borrar junta borra su evento y respuestas`() {
        val (teacherId, groupId) = setup()
        val meetingId = ((createMeeting(groupId).body as Map<*, *>)["id"] as Number).toLong()
        val studentId = TestData(jdbc).student("Ana", groupId, teacherId)
        rest.exchange(
            "/api/v1/meetings/$meetingId/responses/$studentId",
            HttpMethod.PUT,
            HttpEntity(mapOf("response" to "ATTENDING"), HttpHeaders().apply { addAll(headers()) }),
            Map::class.java,
        )
        val response = rest.exchange(
            "/api/v1/meetings/$meetingId",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertEquals(0, jdbc.queryForObject("select count(*) from calendar_events where id = ?", Int::class.java, 1000L + meetingId))
        assertEquals(0, jdbc.queryForObject("select count(*) from meeting_responses where meeting_id = ?", Int::class.java, meetingId))
    }

    @Test
    fun `actualizar junta conserva su id y actualiza su evento de calendario`() {
        val (_, groupId) = setup()
        val meetingId = ((createMeeting(groupId).body as Map<*, *>)["id"] as Number).toLong()
        val response = rest.exchange(
            "/api/v1/meetings/$meetingId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf(
                    "title" to "Junta actualizada", "dateText" to "2026-12-11",
                    "time" to "9:00 am", "place" to "Aula 2",
                    "link" to "https://meet.example.com/y", "reminder" to false,
                ),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals(meetingId, (body["id"] as Number).toLong())
        assertEquals("Junta actualizada", body["title"])
        assertEquals("2026-12-11", body["date"])
        assertEquals("Aula 2", body["location"])
        assertEquals(
            1,
            jdbc.queryForObject(
                "select count(*) from calendar_events where id = ? and date = date '2026-12-11' and title = ?",
                Int::class.java, 1000L + meetingId, "Junta actualizada",
            ),
        )
    }

    @Test
    fun `actualizar junta a fecha hora y lugar de otra devuelve 409`() {
        val (_, groupId) = setup()
        assertEquals(HttpStatus.CREATED, createMeeting(groupId).statusCode)
        val otherId = ((createMeeting(groupId, time = "11:00 am", place = "Aula 2").body as Map<*, *>)["id"] as Number).toLong()
        val response = rest.exchange(
            "/api/v1/meetings/$otherId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf(
                    "title" to "Otra junta", "dateText" to "2026-12-10",
                    "time" to "8:00 AM", "place" to "aula 1", "reminder" to true,
                ),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `crear junta con url invalida devuelve 400`() {
        val (_, groupId) = setup()
        val bad = rest.exchange(
            "/api/v1/meetings",
            HttpMethod.POST,
            HttpEntity(
                mapOf("title" to "Junta", "dateText" to "2026-12-10", "time" to "8:00", "place" to "Aula", "link" to "ftp://x", "groupId" to groupId),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.BAD_REQUEST, bad.statusCode)
    }
}
