package com.example.librerias.data.remote

import com.example.librerias.domain.repository.MeetingRepository
import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.MEETING_ID_OFFSET
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Juntas, respuestas de familias y recordatorios contra la API.
 *
 * Implementa [MeetingRepository] sobre HTTP (M1-M9) y mapea `MeetingDto`/`FamilyResponseDto` a sus
 * modelos de dominio con `toModel`. Mantiene cachés [RemoteListStore] de juntas, asistencia por junta
 * y respuestas por alumno (como pares, porque la caché solo guarda listas). Con [groups] completa
 * las familias que aún no han respondido y con [calendar] refresca o retira el evento asociado: los
 * eventos de junta usan id `1000 + meetings.id` (`MEETING_ID_OFFSET`). Las mutaciones propagan
 * `ApiException`; los errores al cargar un flujo se ignoran y conservan la caché anterior.
 */
class RemoteMeetingRepository(
    private val api: ApiClient,
    private val groups: RemoteGroupRepository? = null,
    private val calendar: RemoteCalendarRepository? = null,
) : MeetingRepository {

    private val meetings = RemoteListStore<Unit, Meeting>()
    private val attendance = RemoteListStore<Long, FamilyResponse>()
    // Se guarda como lista de pares porque RemoteListStore trabaja con listas.
    private val responses = RemoteListStore<Long, Pair<Long, MeetingResponse>>()

    /**
     * `GET meetings?page=&size=` (M1): mapea `PageDto<MeetingDto>` a [Meeting] y cachea la lista bajo
     * la clave `Unit` (única para todas las juntas). Si falla la carga, el flujo mantiene el valor
     * anterior en lugar de propagar el error.
     */
    override fun observeMeetings(): Flow<List<Meeting>> =
        meetings.flow(Unit) {
            api.get<PageDto<MeetingDto>>("meetings?page=0&size=100").items.map { it.toModel() }
        }

    /**
     * `GET meetings/{id}/attendance` (M5): mapea cada `FamilyResponseDto` a [FamilyResponse] y lo
     * cachea por `meetingId`. Si hay repositorio de grupos, consulta `GET meetings/{id}` (M2) y
     * `GET groups/{id}/students` (G5) para añadir como PENDING a los alumnos que aún no respondieron;
     * si alguna de esas consultas falla, se muestra solo lo que devolvió M5. Errores de carga de M5
     * → se conserva la caché.
     */
    override fun observeAttendance(meetingId: Long): Flow<List<FamilyResponse>> =
        attendance.flow(meetingId) {
            val answered: List<FamilyResponse> =
                api.get<PageDto<FamilyResponseDto>>("meetings/$meetingId/attendance?page=0&size=100")
                    .items.map { it.toModel() }

            val groupRepository = groups
            if (groupRepository == null) {
                answered
            } else {
                val meeting = runCatching { api.get<MeetingDto>("meetings/$meetingId").toModel() }.getOrNull()
                if (meeting == null) {
                    answered
                } else {
                    val byStudent = answered.associateBy { it.studentId }
                    groupRepository.observeStudentsOfGroup(meeting.groupId).first().map { student ->
                        byStudent[student.id]
                            ?: FamilyResponse("Familia de ${student.name}", MeetingResponse.PENDING, student.id)
                    }
                }
            }
        }

    /**
     * `GET meetings/responses?studentId=` (M7): convierte las claves de texto a `Long` y los valores
     * a [MeetingResponse] por literal. Cachea pares `(junta, respuesta)` por `studentId` y los
     * convierte a mapa al emitir. Los ids no numéricos se descartan; un fallo de red conserva la caché.
     */
    override fun observeResponses(studentId: Long): Flow<Map<Long, MeetingResponse>> =
        responses.flow(studentId) {
            api.get<ResponsesDto>("meetings/responses?studentId=$studentId").responses
                .mapNotNull { (meetingId, response) ->
                    meetingId.toLongOrNull()?.to(response.toMeetingResponse())
                }
        }.map { it.toMap() }

    /**
     * `POST meetings` (M3): envía [CreateMeetingRequestDto], mapea la junta guardada y la inserta o
     * reemplaza en la caché de juntas. El backend crea el evento de calendario `1000 + id` en la
     * misma transacción. Propaga errores de la API.
     */
    override suspend fun addMeeting(meeting: Meeting): Meeting {
        val dto: MeetingDto = api.post("meetings", meeting.toCreateRequest())
        val saved = dto.toModel()
        meetings.update(Unit) { current ->
            if (current.any { it.id == saved.id }) current.map { if (it.id == saved.id) saved else it }
            else current + saved
        }
        return saved
    }

    /**
     * `PUT meetings/{id}` (M9): envía [UpdateMeetingRequestDto], mapea la junta actualizada y
     * reemplaza su entrada en la caché. Después refresca la caché de calendario (si está disponible)
     * porque el backend actualiza el evento `1000 + id`. Propaga errores de la API.
     */
    override suspend fun updateMeeting(meeting: Meeting): Meeting {
        val dto: MeetingDto = api.put("meetings/${meeting.id}", meeting.toUpdateRequest())
        val saved = dto.toModel()
        meetings.update(Unit) { current -> current.map { if (it.id == saved.id) saved else it } }
        calendar?.refreshEvents()
        return saved
    }

    /**
     * `PUT meetings/{id}/responses/{studentId}` (M6): envía [SetResponseRequestDto] con el literal
     * del enum. Vuelve a cargar la asistencia de la junta, actualiza las respuestas cacheadas de todos
     * los alumnos para ese `meetingId` y refresca el calendario. Propaga errores de la API.
     */
    override suspend fun setResponse(meetingId: Long, studentId: Long, response: MeetingResponse) {
        api.put<MeetingDto>(
            "meetings/$meetingId/responses/$studentId",
            SetResponseRequestDto(response.name),
        )
        attendance.refresh(meetingId)
        responses.updateAll { current ->
            current.map { (id, value) -> if (id == meetingId) id to response else id to value }
        }
        calendar?.refreshEvents()
    }

    /**
     * `POST meetings/{id}/reminders` (M8): lee [PendingDto] y devuelve el número de familias
     * pendientes. No modifica las cachés; propaga errores de la API.
     */
    override suspend fun remindPending(meetingId: Long): Int =
        api.post<PendingDto>("meetings/$meetingId/reminders").pending

    /**
     * `DELETE meetings/{id}` (M4, sin cuerpo): elimina la junta de la caché, vacía su asistencia,
     * retira esa junta de las respuestas cacheadas de todos los alumnos y elimina del calendario el
     * evento con id `MEETING_ID_OFFSET + meetingId`. Propaga errores de la API.
     */
    override suspend fun deleteMeeting(meetingId: Long) {
        api.deleteNoContent("meetings/$meetingId")
        meetings.update(Unit) { current -> current.filterNot { it.id == meetingId } }
        attendance.update(meetingId) { emptyList() }
        responses.updateAll { current -> current.filterNot { (id, _) -> id == meetingId } }
        calendar?.removeLocalEvent(MEETING_ID_OFFSET + meetingId)
    }
}
