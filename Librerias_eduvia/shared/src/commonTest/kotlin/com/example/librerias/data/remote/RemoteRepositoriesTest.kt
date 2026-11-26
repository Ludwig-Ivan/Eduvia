package com.example.librerias.data.remote

import com.example.librerias.model.AppResult
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.User
import com.example.librerias.model.UserRole
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Capa de repositorios remotos: protege el mapeo DTO → modelo y los bodies/rutas exactos enviados a la API; cubre desajustes entre app y backend. */
class RemoteRepositoriesTest {

    @Test
    fun login_guarda_tokens_y_mapea_el_rol() = runBlocking {
        val api = testApi { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/auth/login", request.url.encodedPath)
            jsonResponse(LOGIN_JSON)
        }
        val repo = RemoteAuthRepository(api, api.session)

        val result = repo.login("laura@eduvia.mx", "Maestra123")

        val success = assertIs<AppResult.Success<*>>(result)
        assertEquals(UserRole.TEACHER, (success.value as User).role)
        assertEquals("access-1", api.session.token.value)
        assertEquals("refresh-1", api.session.refreshToken.value)
        assertEquals(1L, api.session.currentUser.value?.id)
    }

    @Test
    fun register_devuelve_el_detail_del_conflicto_como_mensaje() = runBlocking {
        val problem = PROBLEM_JSON.replace("\"detail\": \"Grupo no encontrado\"", "\"detail\": \"El correo ya está registrado\"")
        val api = testApi { jsonResponse(problem, HttpStatusCode.Conflict) }
        val repo = RemoteAuthRepository(api, api.session)

        val result = repo.register(UserRole.PARENT, "María", "maria@correo.com", "5512345678", "Padre1234")

        val error = assertIs<AppResult.Error>(result)
        assertEquals("El correo ya está registrado", error.message)
    }

    @Test
    fun groups_mapea_la_page_y_findByCode_devuelve_null_en_404() = runBlocking {
        val api = testApi { request ->
            when {
                request.url.encodedPath.endsWith("/groups/by-code") ->
                    jsonResponse(PROBLEM_JSON, HttpStatusCode.NotFound)
                else -> jsonResponse(pageOf(GROUP_JSON))
            }
        }
        val repo = RemoteGroupRepository(api)

        val groups = repo.observeGroupsOf(1).first()

        assertEquals(1, groups.size)
        assertEquals("Matemáticas 6A", groups.first().name)
        assertEquals(1L, groups.first().teacherId)
        assertEquals(null, repo.findByCode("NO-EXISTE"))
    }

    @Test
    fun tasks_mapea_fechas_y_entregas_y_addTask_envia_el_body_del_contrato() = runBlocking {
        var sentJson: String? = null
        val api = testApi { request ->
            if (request.method == HttpMethod.Post) {
                sentJson = (request.body as TextContent).text
                jsonResponse(TASK_JSON, HttpStatusCode.Created)
            } else {
                jsonResponse(pageOf(TASK_JSON))
            }
        }
        val repo = RemoteTaskRepository(api)

        val tasks = repo.observeTasks(1).first()
        val created = repo.addTask(tasks.first())
        val sentBody = api.json.decodeFromString<CreateTaskRequestDto>(sentJson!!)

        assertEquals(CalendarDate(2026, 12, 15), tasks.first().dueDate)
        assertEquals(setOf(7L, 8L), tasks.first().deliveredStudentIds)
        assertEquals(5L, created.id)
        assertEquals(1L, sentBody.groupId)
        assertEquals("2026-12-15", sentBody.dateText)
    }

    @Test
    fun meetings_mapea_id_y_respuesta_y_setResponse_envia_el_enum() = runBlocking {
        var sentResponse = ""
        val api = testApi { request ->
            when (request.method) {
                HttpMethod.Get -> jsonResponse(pageOf(MEETING_JSON))
                HttpMethod.Put -> {
                    sentResponse = request.body.toString()
                    jsonResponse(MEETING_JSON)
                }
                else -> jsonResponse(MEETING_JSON)
            }
        }
        val repo = RemoteMeetingRepository(api)

        val meetings = repo.observeMeetings().first()
        repo.setResponse(3L, 7L, MeetingResponse.ATTENDING)

        assertEquals(3L, meetings.first().id)
        assertEquals(CalendarDate(2026, 12, 10), meetings.first().date)
        assertEquals(MeetingResponse.ATTENDING, meetings.first().response)
        assertTrue(sentResponse.contains("ATTENDING"))
    }

    @Test
    fun meetings_updateMeeting_envia_PUT_y_actualiza_la_cache() = runBlocking {
        var sentPath = ""
        var sentJson: String? = null
        val updatedJson = MEETING_JSON.replace("Junta de padres", "Junta editada")
        val api = testApi { request ->
            when (request.method) {
                HttpMethod.Get -> jsonResponse(pageOf(MEETING_JSON))
                HttpMethod.Put -> {
                    sentPath = request.url.encodedPath
                    sentJson = (request.body as TextContent).text
                    jsonResponse(updatedJson)
                }
                else -> jsonResponse(MEETING_JSON)
            }
        }
        val repo = RemoteMeetingRepository(api)
        val titles = mutableListOf<String>()
        val collector = launch { repo.observeMeetings().collect { titles.add(it.first().title) } }
        while (titles.isEmpty()) yield()
        val original = repo.observeMeetings().first().first()

        val updated = repo.updateMeeting(original.copy(title = "Junta editada"))
        val sentBody = api.json.decodeFromString<UpdateMeetingRequestDto>(sentJson!!)
        while (titles.size < 2) yield()
        collector.cancel()

        assertEquals("/api/v1/meetings/3", sentPath)
        assertEquals(3L, updated.id)
        assertEquals("Junta editada", updated.title)
        assertEquals("Junta editada", titles.last())
        assertEquals("2026-12-10", sentBody.dateText)
        assertEquals("Aula 1", sentBody.place)
    }

    @Test
    fun calendar_addEvent_de_TASK_no_hace_red_y_addEvent_de_EXAM_si() = runBlocking {
        var requests = 0
        val api = testApi { request ->
            requests++
            assertEquals(HttpMethod.Post, request.method)
            jsonResponse(
                """{"id":9,"groupId":1,"date":"2026-12-20","type":"EXAM","title":"Examen","subtitle":"","attendance":null}""",
                HttpStatusCode.Created,
            )
        }
        val repo = RemoteCalendarRepository(api)
        val taskEvent = CalendarEvent(
            id = 5L,
            groupId = 1L,
            date = CalendarDate(2026, 12, 15),
            type = CalendarEventType.TASK,
            title = "Fracciones",
            subtitle = "MATEMATICAS",
        )

        repo.addEvent(taskEvent)
        assertEquals(0, requests)

        repo.addEvent(taskEvent.copy(id = 9L, type = CalendarEventType.EXAM, date = CalendarDate(2026, 12, 20)))
        assertEquals(1, requests)
    }

    @Test
    fun calendar_mapea_el_evento_de_junta_con_id_1000_mas() = runBlocking {
        val api = testApi { jsonResponse(CALENDAR_PAGE_JSON) }
        val repo = RemoteCalendarRepository(api)

        val events = repo.observeEvents(UserRole.TEACHER).first()

        assertEquals(2, events.size)
        assertEquals(1003L, events.first { it.type == CalendarEventType.MEETING }.id)
        assertEquals(3, events.first { it.type == CalendarEventType.MEETING }.attendance?.confirmed)
    }
}
