package com.example.librerias.contract

import com.example.librerias.data.remote.AttendanceDto
import com.example.librerias.data.remote.CalendarEventDto
import com.example.librerias.data.remote.FamilyResponseDto
import com.example.librerias.data.remote.GroupDto
import com.example.librerias.data.remote.MeetingDto
import com.example.librerias.data.remote.PageDto
import com.example.librerias.data.remote.ProblemDto
import com.example.librerias.data.remote.StudentDto
import com.example.librerias.data.remote.TaskDto
import com.example.librerias.data.remote.TokenResponseDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Usa los MISMOS archivos que genera el backend; ignoreUnknownKeys=false detecta campos nuevos.
 *
 * Capa de contrato: lee los JSON de `contract/fixtures` y los deserializa con los DTOs de la app.
 * Al rechazar claves desconocidas, cualquier campo nuevo o renombrado en la API falla en lugar
 * de descartarse en silencio: así se detectan cambios de forma entre backend y cliente.
 */
class ContractFixturesFileTest {

    private val dir = File(
        System.getProperty("contract.fixtures.dir")
            ?: File("../../contract/fixtures").absolutePath,
    )

    private val strictJson = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        encodeDefaults = true
    }

    private fun fixture(name: String): String = dir.resolve("$name.json").readText()

    @Test
    fun login_y_register_se_deserializan_con_el_DTO_de_la_app() {
        val login = strictJson.decodeFromString<TokenResponseDto>(fixture("login-response"))
        val register = strictJson.decodeFromString<TokenResponseDto>(fixture("register-response"))

        assertEquals(1L, login.user?.id)
        assertEquals("TEACHER", login.user?.role)
        assertEquals(4L, register.user?.id)
        assertEquals("PARENT", register.user?.role)
    }

    @Test
    fun listados_y_detalles_se_deserializan_con_el_DTO_de_la_app() {
        val groups = strictJson.decodeFromString<PageDto<GroupDto>>(fixture("groups-page"))
        val students = strictJson.decodeFromString<PageDto<StudentDto>>(fixture("students-page"))
        val tasks = strictJson.decodeFromString<PageDto<TaskDto>>(fixture("tasks-page"))
        val task = strictJson.decodeFromString<TaskDto>(fixture("task-detail"))
        val meetings = strictJson.decodeFromString<PageDto<MeetingDto>>(fixture("meetings-page"))
        val meeting = strictJson.decodeFromString<MeetingDto>(fixture("meeting-detail"))
        val calendar = strictJson.decodeFromString<PageDto<CalendarEventDto>>(fixture("calendar-page"))
        val attendance = strictJson.decodeFromString<PageDto<FamilyResponseDto>>(fixture("attendance-page"))

        assertEquals("AB-1234", groups.items.first().code)
        assertEquals(0L, students.items.last().parentId)
        assertEquals("2026-12-15", tasks.items.first().dueDate)
        assertEquals("2026-12-15", task.dueDate)
        assertEquals("8:00", meetings.items.first().time)
        assertEquals(1L, meeting.id)
        assertEquals(1001L, calendar.items.last().id)
        assertEquals(AttendanceDto(3, 1, 6), calendar.items.last().attendance)
        assertEquals("María García", attendance.items.first().familyName)
    }

    @Test
    fun el_problem_detail_incluye_instance_y_errores() {
        val problem = strictJson.decodeFromString<ProblemDto>(fixture("problem-validation"))

        assertEquals(400, problem.status)
        assertEquals("/api/v1/auth/register", problem.instance)
        assertEquals(2, problem.errors.size)
        assertTrue(problem.errors.any { it.field == "acceptedTerms" })
    }
}
