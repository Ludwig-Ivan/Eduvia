package com.example.librerias.api.service

import com.example.librerias.api.domain.CalendarEvent
import com.example.librerias.api.domain.CalendarEventType
import com.example.librerias.api.domain.MeetingResponse
import com.example.librerias.api.domain.UserRole
import com.example.librerias.api.repository.CalendarEventRepository
import com.example.librerias.api.repository.MeetingRepository
import com.example.librerias.api.repository.MeetingResponseRepository
import com.example.librerias.api.repository.SchoolGroupRepository
import com.example.librerias.api.web.notFound
import com.example.librerias.api.web.dto.AttendanceSummaryDto
import com.example.librerias.api.web.dto.PageResponse
import com.example.librerias.api.web.dto.toDto
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Servicio del calendario unificado de la app: lista, crea y borra eventos y calcula la asistencia
 * que la vista del docente muestra para las juntas.
 *
 * Responsabilidad: mezclar en una sola lista los eventos directos, los generados por tareas (con
 * `calendar_events.id = school_tasks.id`) y los generados por juntas (con `id = 1000 + meetings.id`,
 * para evitar colisiones con los ids de tareas que se solapaban en las dos listas de la app). La
 * asistencia se calcula solo para el docente; la app espera `attendance: null` explícito para el
 * rol PARENT.
 *
 * Colaboradores: CalendarEventRepository y SchoolGroupRepository para eventos y su grupo;
 * MeetingRepository y MeetingResponseRepository para derivar `confirmed`, `declined` y `pending`;
 * JdbcTemplate para reservar el siguiente id libre de la secuencia de eventos directos.
 */
@Service
class CalendarService(
    private val events: CalendarEventRepository,
    private val groups: SchoolGroupRepository,
    private val meetings: MeetingRepository,
    private val responses: MeetingResponseRepository,
    private val jdbc: JdbcTemplate,
) {
    /**
     * Devuelve una página de eventos ordenados por fecha. La paginación se resuelve aquí porque
     * conviven ids de tres orígenes en una sola tabla y la app solo consume `items`; el resumen de
     * asistencia se adjunta únicamente cuando quien consulta es docente.
     *
     * @param role rol del usuario autenticado, que decide si se calcula la asistencia.
     * @param pageable página y tamaño solicitados por el controlador.
     * @return PageResponse con los eventos y sus metadatos de paginación.
     */
    fun list(role: UserRole, pageable: Pageable): PageResponse<com.example.librerias.api.web.dto.CalendarEventDto> {
        val all = events.findAll().sortedBy { it.date }
        val offset = pageable.offset.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val slice = if (offset < all.size) all.drop(offset).take(pageable.pageSize) else emptyList()
        val totalPages = if (all.isEmpty()) 0 else (all.size + pageable.pageSize - 1) / pageable.pageSize
        return PageResponse(
            items = slice.map { event -> event.toDto(if (role == UserRole.TEACHER) attendanceOf(event) else null) },
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = all.size.toLong(),
            totalPages = totalPages,
        )
    }

    /**
     * Crea un evento directo (exámenes u otros recordatorios) en la misma transacción en la que se
     * comprueba el grupo. El id se toma de `calendar_events_id_seq`, la secuencia que también usan
     * las tareas, de modo que no pueda chocar con el rango desplazado reservado a las juntas.
     *
     * @param groupId grupo al que pertenece el evento.
     * @param date fecha del evento en el formato ISO que usa la app.
     * @param type tipo de evento (`TASK`, `MEETING` o `EXAM`).
     * @param title título mostrado en la agenda.
     * @param subtitle texto secundario de la agenda.
     * @return CalendarEventDto del evento recién creado.
     * @throws ApiException 404 si el grupo no existe; un evento huérfano no aparecería en la agenda
     * de ningún usuario de la app KMP.
     */
    @Transactional
    fun create(
        groupId: Long,
        date: java.time.LocalDate,
        type: CalendarEventType,
        title: String,
        subtitle: String,
    ): com.example.librerias.api.web.dto.CalendarEventDto {
        if (!groups.existsById(groupId)) throw notFound("Grupo no encontrado")
        return events.save(
            CalendarEvent(
                id = jdbc.queryForObject("select nextval('calendar_events_id_seq')", Long::class.java)!!,
                groupId = groupId,
                date = date,
                type = type,
                title = title.trim(),
                subtitle = subtitle.trim(),
            ),
        ).toDto()
    }

    /**
     * Elimina un evento del calendario; el controlador traduce el resultado a 204, que es lo que
     * espera `deleteNoContent` de la app.
     *
     * @param id identificador del evento (el de una tarea o `1000 + id` en el caso de una junta).
     * @return Unit.
     * @throws ApiException 404 si el evento no existe, para no borrar silenciosamente algo que la
     * app todavía está mostrando.
     */
    @Transactional
    fun delete(id: Long) {
        val event = events.findById(id).orElseThrow { notFound("Evento no encontrado") }
        events.delete(event)
    }

    private fun attendanceOf(event: CalendarEvent): AttendanceSummaryDto? {
        if (event.type != CalendarEventType.MEETING || event.id <= CalendarEvent.MEETING_ID_OFFSET) return null
        val meetingId = event.id - CalendarEvent.MEETING_ID_OFFSET
        if (!meetings.existsById(meetingId)) return null
        return AttendanceSummaryDto(
            confirmed = responses.countByMeetingIdAndResponse(meetingId, MeetingResponse.ATTENDING),
            declined = responses.countByMeetingIdAndResponse(meetingId, MeetingResponse.DECLINED),
            pending = responses.countByMeetingIdAndResponse(meetingId, MeetingResponse.PENDING),
        )
    }
}
