package com.example.librerias.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integración HTTP del calendario (operaciones C1 a C3 de `docs/backend/api-contract.md`).
 *
 * Cubre la capa web y de persistencia: riesgo de que el CRUD directo de eventos no persista en
 * PostgreSQL, de que la vista de docente incluya `attendance` y la de padre no (C1 y regla 7), o de
 * que un `role` distinto de `PARENT` o `TEACHER` no se rechace con 400.
 */
class CalendarIntegrationTest : IntegrationTestBase() {

    private fun setup(): Pair<Long, Long> {
        val teacherId = TestData(jdbc).user(passwordHash = PasswordEncoderForTest.encodeForTest("Password1!"))
        val groupId = TestData(jdbc).group(teacherId, "A", "AB-1234", studentCount = 2)
        return teacherId to groupId
    }

    private fun headers() = authHeaders(tokenFor(password = "Password1!"))

    @Test
    fun `crear evento de examen y borrarlo`() {
        val (_, groupId) = setup()
        val created = rest.exchange(
            "/api/v1/calendar/events",
            HttpMethod.POST,
            HttpEntity(
                mapOf("groupId" to groupId, "date" to "2026-12-15", "type" to "EXAM", "title" to "Examen", "subtitle" to "Final"),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.CREATED, created.statusCode)
        val id = ((created.body as Map<*, *>)["id"] as Number).toLong()
        assertEquals("2026-12-15", (created.body as Map<*, *>)["date"])

        val deleted = rest.exchange(
            "/api/v1/calendar/events/$id",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, deleted.statusCode)
        assertEquals(0, jdbc.queryForObject("select count(*) from calendar_events where id = ?", Int::class.java, id))
    }

    // Se insertan junta y respuesta por SQL para verificar que el resumen se deriva, no se almacena.
    @Test
    fun `vista de docente incluye attendance y vista de padre no`() {
        val (teacherId, groupId) = setup()
        jdbc.queryForObject(
            """insert into meetings(group_id,title,date,time,location)
               values (?,?,?,?,?) returning id""",
            Long::class.java,
            groupId, "Junta", java.sql.Date.valueOf("2026-12-10"), "8:00", "Aula",
        )
        val id = jdbc.queryForObject("select max(id) from meetings", Long::class.java)!!
        val studentId = TestData(jdbc).student("Ana", groupId, teacherId)
        jdbc.update(
            "insert into meeting_responses(meeting_id,student_id,response) values (?,?,?)",
            id, studentId, "ATTENDING",
        )
        val eventId = 1000L + id
        jdbc.update(
            """insert into calendar_events(id,group_id,date,type,title,subtitle)
               values (?,?,?,?,?,?)""",
            eventId, groupId, java.sql.Date.valueOf("2026-12-10"), "MEETING", "Junta", "8:00",
        )

        val teacher = rest.exchange(
            "/api/v1/calendar/events?role=TEACHER",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, teacher.statusCode)
        val event = ((teacher.body as Map<*, *>)["items"] as List<Map<*, *>>).first { (it["id"] as Number).toLong() == eventId }
        val attendance = event["attendance"] as Map<*, *>
        assertEquals(1, (attendance["confirmed"] as Number).toInt())
        assertEquals(0, (attendance["pending"] as Number).toInt())

        val parent = rest.exchange(
            "/api/v1/calendar/events?role=PARENT",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, parent.statusCode)
        val parentEvent = ((parent.body as Map<*, *>)["items"] as List<Map<*, *>>).first { (it["id"] as Number).toLong() == eventId }
        assertTrue(parentEvent["attendance"] == null)
    }

    @Test
    fun `role invalido devuelve 400`() {
        setup()
        val response = rest.exchange(
            "/api/v1/calendar/events?role=DIRECTOR",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}
