package com.example.librerias.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

/**
 * Integración HTTP de tareas (operaciones T1 a T6 de `docs/backend/api-contract.md`).
 *
 * Cubre la capa web, de servicio y la transacción contra PostgreSQL: el riesgo es que la tarea y su
 * `CalendarEvent` con el mismo id dejen de sincronizarse al crear, editar o borrar (regla 3), que el
 * orden por `dueDate` o la paginación fallen, que el `status` derivado sea incorrecto o que una
 * entrega se duplique o acepte alumnos de otro grupo (regla 6).
 */
class TaskIntegrationTest : IntegrationTestBase() {

    private fun setup(): Pair<Long, Long> {
        val teacherId = TestData(jdbc).user(passwordHash = PasswordEncoderForTest.encodeForTest("Password1!"))
        val groupId = TestData(jdbc).group(teacherId, "A", "AB-1234", studentCount = 2)
        return teacherId to groupId
    }

    private fun headers() = authHeaders(tokenFor(password = "Password1!"))

    private fun createTask(groupId: Long, title: String = "Tarea 1", date: String = "2026-12-01") =
        rest.exchange(
            "/api/v1/tasks",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "groupId" to groupId, "title" to title, "subject" to "MATEMATICAS",
                    "description" to "Hacer ejercicios", "dateText" to date,
                ),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )

    @Test
    fun `crear tarea crea evento de calendario con el mismo id`() {
        val (_, groupId) = setup()
        val response = createTask(groupId)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        val id = (body["id"] as Number).toLong()
        assertEquals("2026-12-01", body["dueDate"])
        assertEquals("MATEMATICAS", body["subject"])
        assertTrue((body["deliveredStudentIds"] as List<*>).isEmpty())
        assertEquals(
            1,
            jdbc.queryForObject("select count(*) from calendar_events where id = ? and type = 'TASK'", Int::class.java, id),
        )
        assertEquals(2, jdbc.queryForObject("select total_students from school_tasks where id = ?", Int::class.java, id))
    }

    @Test
    fun `listar tareas por grupo pagina y ordena por fecha`() {
        val (_, groupId) = setup()
        createTask(groupId, "Tarea tardía", "2026-12-20")
        createTask(groupId, "Tarea pronto", "2026-11-25")
        val response = rest.exchange(
            "/api/v1/tasks?groupId=$groupId&page=0&size=1",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals(2L, (body["totalElements"] as Number).toLong())
        assertEquals(2, body["totalPages"])
        val first = (body["items"] as List<Map<*, *>>)[0]
        assertEquals("Tarea pronto", first["title"])
    }

    // Se fuerza una fecha pasada para comprobar el caso `EXPIRED` de `TaskStatus`.
    @Test
    fun `detalle calcula estado segun entregas y fecha`() {
        val (_, groupId) = setup()
        val id = (createTask(groupId, date = "2020-01-01").body as Map<*, *>)["id"] as Number
        val detail = rest.exchange(
            "/api/v1/tasks/$id",
            HttpMethod.GET,
            HttpEntity<Void>(headers()),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, detail.statusCode)
        assertEquals("EXPIRED", (detail.body as Map<*, *>)["status"])
    }

    @Test
    fun `actualizar tarea sincroniza el evento de calendario`() {
        val (_, groupId) = setup()
        val id = (createTask(groupId).body as Map<*, *>)["id"] as Number
        val response = rest.exchange(
            "/api/v1/tasks/$id",
            HttpMethod.PUT,
            HttpEntity(
                mapOf("title" to "Nuevo título", "subject" to "CIENCIAS", "description" to "Nueva", "dateText" to "2026-12-31"),
                HttpHeaders().apply { addAll(headers()) },
            ),
            Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Nuevo título", (response.body as Map<*, *>)["title"])
        assertEquals(
            "2026-12-31",
            jdbc.queryForObject("select date::text from calendar_events where id = ?", String::class.java, id.toLong()),
        )
    }

    @Test
    fun `marcar entregada es idempotente y valida pertenencia al grupo`() {
        val (_, groupId) = setup()
        val id = ((createTask(groupId).body as Map<*, *>)["id"] as Number).toLong()
        val studentId = TestData(jdbc).student("Ana", groupId)
        val otherGroup = TestData(jdbc).group(1, "B", "BB-0002")
        val outsider = TestData(jdbc).student("Luis", otherGroup)

        repeat(2) {
            val response = rest.exchange(
                "/api/v1/tasks/$id/deliveries",
                HttpMethod.POST,
                HttpEntity(mapOf("studentId" to studentId), HttpHeaders().apply { addAll(headers()) }),
                Map::class.java,
            )
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(1, ((response.body as Map<*, *>)["deliveredStudentIds"] as List<*>).size)
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from task_deliveries", Int::class.java))

        val wrong = rest.exchange(
            "/api/v1/tasks/$id/deliveries",
            HttpMethod.POST,
            HttpEntity(mapOf("studentId" to outsider), HttpHeaders().apply { addAll(headers()) }),
            Map::class.java,
        )
        assertEquals(HttpStatus.CONFLICT, wrong.statusCode)
    }

    @Test
    fun `borrar tarea borra su evento`() {
        val (_, groupId) = setup()
        val id = ((createTask(groupId).body as Map<*, *>)["id"] as Number).toLong()
        val response = rest.exchange(
            "/api/v1/tasks/$id",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers()),
            Void::class.java,
        )
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertEquals(0, jdbc.queryForObject("select count(*) from calendar_events where id = ?", Int::class.java, id))
        val missing = rest.exchange("/api/v1/tasks/$id", HttpMethod.GET, HttpEntity<Void>(headers()), Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, missing.statusCode)
    }

    @Test
    fun `crear tarea con fecha invalida devuelve 400`() {
        val (_, groupId) = setup()
        val response = createTask(groupId, date = "01-12-2026")
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertTrue((response.body as Map<*, *>)["errors"] != null)
    }
}
