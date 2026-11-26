package com.example.librerias.api.service

import com.example.librerias.api.domain.CalendarEvent
import com.example.librerias.api.domain.CalendarEventType
import com.example.librerias.api.domain.Meeting
import com.example.librerias.api.domain.MeetingResponse
import com.example.librerias.api.domain.MeetingResponseEntity
import com.example.librerias.api.repository.CalendarEventRepository
import com.example.librerias.api.repository.MeetingRepository
import com.example.librerias.api.repository.MeetingResponseRepository
import com.example.librerias.api.repository.SchoolGroupRepository
import com.example.librerias.api.web.badRequest
import com.example.librerias.api.web.conflict
import com.example.librerias.api.web.notFound
import com.example.librerias.api.web.dto.FamilyResponseDto
import com.example.librerias.api.web.dto.FieldErrorDto
import com.example.librerias.api.web.dto.PageResponse
import com.example.librerias.api.web.dto.PendingDto
import com.example.librerias.api.web.dto.ResponsesDto
import com.example.librerias.api.web.dto.toDto
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Servicio de juntas (reuniones docente-familia), de las respuestas de asistencia y de los
 * recordatorios de las familias que falta responder.
 *
 * Responsabilidad: impedir dos juntas para el mismo grupo, fecha, hora y lugar (regla que la app
 * traduce a 409) y mantener sincronizado el evento de calendario `1000 + meetings.id` al crear,
 * editar y borrar, además de agregar las respuestas para el listado y el detalle.
 *
 * Colaboradores: MeetingRepository, SchoolGroupRepository, MeetingResponseRepository,
 * CalendarEventRepository y StudentRepository. Las escrituras comparten transacción para que una
 * junta y su evento nunca queden desincronizados.
 */
@Service
class MeetingService(
    private val meetings: MeetingRepository,
    private val groups: SchoolGroupRepository,
    private val responses: MeetingResponseRepository,
    private val events: CalendarEventRepository,
    private val studentRepository: com.example.librerias.api.repository.StudentRepository,
) {
    /**
     * Lista las juntas paginadas; el controlador las ordena por fecha y el DTO calcula si es la
     * próxima junta y su etiqueta relativa, tal como las muestra la app.
     *
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `MeetingDto`, con la respuesta agregada de cada junta.
     */
    fun list(pageable: Pageable): PageResponse<com.example.librerias.api.web.dto.MeetingDto> {
        val page = meetings.findAllBy(pageable)
        return PageResponse(
            items = page.content.map { it.toDto(response = responseOf(it.id)) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    /**
     * Devuelve una junta concreta con su respuesta agregada; la app la abre al entrar en el detalle.
     *
     * @param id identificador de la junta.
     * @return `MeetingDto` con los datos de la junta y su estado de asistencia.
     * @throws ApiException 404 si la junta no existe.
     */
    fun get(id: Long): com.example.librerias.api.web.dto.MeetingDto {
        val meeting = meetings.findById(id).orElseThrow { notFound("Junta no encontrada") }
        return meeting.toDto(response = responseOf(id))
    }

    /**
     * Crea una junta y su evento de calendario en una sola transacción. El evento usa
     * `1000 + meetings.id` porque en la app las juntas y las tareas vivían en listas con ids
     * solapados y en una única tabla hay que desambiguarlos. La unicidad por grupo, fecha, hora y
     * lugar responde al contrato de la app: no se admiten dos convocatorias en el mismo hueco.
     *
     * @param title título de la junta, obligatorio.
     * @param dateText fecha en formato `YYYY-MM-DD`, exigido por el contrato.
     * @param time hora en texto libre, obligatoria (la app la muestra tal cual).
     * @param place lugar de la junta, obligatorio.
     * @param link enlace opcional, solo `http`/`https` cuando se aporta.
     * @param reminder si la junta debe mostrar recordatorio.
     * @param groupId grupo docente al que pertenece la junta.
     * @return `MeetingDto` de la junta creada.
     * @throws ApiException 400 si faltan título, hora o lugar, si el enlace no es http(s) o si la
     * fecha no sigue el formato `YYYY-MM-DD` que espera la app KMP; el error señala el campo.
     * @throws ApiException 404 si el grupo indicado no existe; sin grupo la junta no tendría
     * agenda donde mostrarse.
     * @throws ApiException 409 si ya hay una junta en esa fecha, hora y lugar para el grupo.
     */
    @Transactional
    fun create(
        title: String,
        dateText: String,
        time: String,
        place: String,
        link: String?,
        reminder: Boolean,
        groupId: Long,
    ): com.example.librerias.api.web.dto.MeetingDto {
        if (title.isBlank()) throw badRequest("Título obligatorio", listOf(FieldErrorDto("title", "Requerido")))
        if (time.isBlank()) throw badRequest("Hora obligatoria", listOf(FieldErrorDto("time", "Requerida")))
        if (place.isBlank()) throw badRequest("Lugar obligatorio", listOf(FieldErrorDto("place", "Requerido")))
        if (!link.isNullOrBlank() && !link.matches(Regex("^https?://.+"))) {
            throw badRequest("Enlace inválido", listOf(FieldErrorDto("link", "Debe ser http(s)")))
        }
        val date = parseDate(dateText)
        if (!groups.existsById(groupId)) throw notFound("Grupo no encontrado")
        if (meetings.findConflictingTimes(groupId, date, place.trim(), -1)
                .any { it == time.trim().lowercase() }
        ) {
            throw conflict("Ya existe una junta en esa fecha, hora y lugar")
        }
        val meeting = meetings.save(
            Meeting(
                groupId = groupId,
                title = title.trim(),
                date = date,
                time = time.trim(),
                location = place.trim(),
                virtualLink = link?.takeIf { it.isNotBlank() },
                reminderEnabled = reminder,
            ),
        )
        events.save(
            CalendarEvent(
                id = CalendarEvent.MEETING_ID_OFFSET + meeting.id,
                groupId = groupId,
                date = date,
                type = CalendarEventType.MEETING,
                title = meeting.title,
                subtitle = meeting.time,
            ),
        )
        return meeting.toDto()
    }

    /**
     * Actualiza una junta conservando su identificador y sincronizando el evento `1000 + id`, para
     * que la agenda no siga mostrando los datos anteriores tras la edición (M9 del contrato). El
     * chequeo de duplicados excluye la propia junta: cambiar solo el título no debe generar un
     * conflicto consigo misma.
     *
     * @param id identificador de la junta a editar.
     * @param title título, obligatorio.
     * @param dateText fecha en `YYYY-MM-DD`.
     * @param time hora obligatoria.
     * @param place lugar obligatorio.
     * @param link enlace opcional, solo http(s).
     * @param reminder si debe mostrar recordatorio.
     * @return `MeetingDto` actualizado, con la respuesta agregada vigente.
     * @throws ApiException 400 si faltan campos obligatorios, el enlace no es http(s) o la fecha no
     * tiene el formato ISO que el formulario de la app envía.
     * @throws ApiException 404 si la junta no existe.
     * @throws ApiException 409 si otra junta del mismo grupo ocupa ya esa fecha, hora y lugar.
     */
    @Transactional
    fun update(
        id: Long,
        title: String,
        dateText: String,
        time: String,
        place: String,
        link: String?,
        reminder: Boolean,
    ): com.example.librerias.api.web.dto.MeetingDto {
        val meeting = meetings.findById(id).orElseThrow { notFound("Junta no encontrada") }
        if (title.isBlank()) throw badRequest("Título obligatorio", listOf(FieldErrorDto("title", "Requerido")))
        if (time.isBlank()) throw badRequest("Hora obligatoria", listOf(FieldErrorDto("time", "Requerida")))
        if (place.isBlank()) throw badRequest("Lugar obligatorio", listOf(FieldErrorDto("place", "Requerido")))
        if (!link.isNullOrBlank() && !link.matches(Regex("^https?://.+"))) {
            throw badRequest("Enlace inválido", listOf(FieldErrorDto("link", "Debe ser http(s)")))
        }
        val date = parseDate(dateText)
        val normalizedTime = time.trim()
        val normalizedPlace = place.trim()
        if (meetings.findConflictingTimes(meeting.groupId, date, normalizedPlace, id)
                .any { it == normalizedTime.lowercase() }
        ) {
            throw conflict("Ya existe una junta en esa fecha, hora y lugar")
        }
        val normalizedLink = link?.takeIf { it.isNotBlank() }
        meeting.title = title.trim()
        meeting.date = date
        meeting.time = normalizedTime
        meeting.location = normalizedPlace
        meeting.virtualLink = normalizedLink
        meeting.hasVirtualLink = normalizedLink != null
        meeting.reminderEnabled = reminder
        meeting.updatedAt = java.time.Instant.now()
        meetings.save(meeting)
        events.findById(CalendarEvent.MEETING_ID_OFFSET + id).ifPresent { event ->
            event.date = date
            event.title = meeting.title
            event.subtitle = meeting.time
            events.save(event)
        }
        return meeting.toDto(response = responseOf(id))
    }

    /**
     * Borra la junta junto con sus respuestas y su evento de calendario en la misma transacción:
     * dejar huérfanos haría que la app contara asistencias de reuniones desaparecidas.
     *
     * @param id identificador de la junta.
     * @return Unit; el controlador devuelve 204 sin cuerpo.
     * @throws ApiException 404 si la junta no existe.
     */
    @Transactional
    fun delete(id: Long) {
        val meeting = meetings.findById(id).orElseThrow { notFound("Junta no encontrada") }
        responses.deleteByMeetingId(id)
        events.findById(CalendarEvent.MEETING_ID_OFFSET + id).ifPresent { events.delete(it) }
        meetings.delete(meeting)
    }

    /**
     * Lista la asistencia por familia recogida en las respuestas. Se pagina aquí porque el número
     * de familias puede ser grande y la app solo necesita una página.
     *
     * @param id junta cuya asistencia se consulta.
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `FamilyResponseDto`, con nombre de la familia, respuesta e id del
     * alumno.
     * @throws ApiException 404 si la junta no existe o si una respuesta apunta a un alumno que ya
     * no está.
     */
    fun attendance(id: Long, pageable: Pageable): PageResponse<FamilyResponseDto> {
        if (!meetings.existsById(id)) throw notFound("Junta no encontrada")
        val all = responses.findAllByMeetingId(id).map { response ->
            val student = studentRepository.findById(response.student.id).orElseThrow { notFound("Alumno no encontrado") }
            FamilyResponseDto(student.name, response.response, student.id)
        }
        val offset = pageable.offset.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val slice = if (offset < all.size) all.drop(offset).take(pageable.pageSize) else emptyList()
        val totalPages = if (all.isEmpty()) 0 else (all.size + pageable.pageSize - 1) / pageable.pageSize
        return PageResponse(slice, pageable.pageNumber, pageable.pageSize, all.size.toLong(), totalPages)
    }

    /**
     * Registra o actualiza la respuesta de una familia: si ya existe se reemplaza y si no se crea.
     * Ese comportamiento de upsert permite a la app reenviar la misma petición sin duplicar filas
     * (la clave primaria es junta + alumno).
     *
     * @param meetingId junta a la que se responde.
     * @param studentId alumno de la familia que responde.
     * @param response `ATTENDING`, `DECLINED` o `PENDING`, tal como los envía la app.
     * @return `MeetingDto` con el estado agregado recalculado.
     * @throws ApiException 404 si la junta o el alumno no existen; sin cualquiera de los dos no se
     * puede construir la clave de la respuesta.
     */
    @Transactional
    fun setResponse(meetingId: Long, studentId: Long, response: MeetingResponse): com.example.librerias.api.web.dto.MeetingDto {
        val meeting = meetings.findById(meetingId).orElseThrow { notFound("Junta no encontrada") }
        val existing = responses.findByMeetingIdAndStudentId(meetingId, studentId)
        if (existing != null) {
            existing.response = response
            existing.updatedAt = java.time.Instant.now()
            responses.save(existing)
        } else {
            responses.save(
                MeetingResponseEntity(
                    meeting = meeting,
                    student = studentRepository.findById(studentId).orElseThrow { notFound("Alumno no encontrado") },
                    response = response,
                ),
            )
        }
        return meeting.toDto(response = responseOf(meetingId))
    }

    /**
     * Devuelve el mapa de respuestas de un alumno indexado por el id de la junta como texto: las
     * claves JSON de un objeto siempre son cadenas y la app KMP lo deserializa como
     * `Map<String, String>`.
     *
     * @param studentId alumno cuyas respuestas se consultan.
     * @return ResponsesDto con el mapa `id de junta -> nombre de la respuesta`.
     */
    fun responsesOf(studentId: Long): ResponsesDto {
        val map = responses.findAllByStudentId(studentId)
            .associate { it.meeting.id.toString() to it.response.name }
        return ResponsesDto(map)
    }

    /**
     * Cuenta cuántas familias siguen sin responder para alimentar el recordatorio de la junta. Se
     * calcula en el momento de la petición, no se almacena, para que nunca quede desactualizado.
     *
     * @param meetingId junta cuyo pendiente se consulta.
     * @return PendingDto con el número de respuestas `PENDING`.
     * @throws ApiException 404 si la junta no existe.
     */
    @Transactional
    fun remindPending(meetingId: Long): PendingDto {
        if (!meetings.existsById(meetingId)) throw notFound("Junta no encontrada")
        val pending = responses.countByMeetingIdAndResponse(meetingId, MeetingResponse.PENDING)
        return PendingDto(pending)
    }

    private fun responseOf(meetingId: Long): MeetingResponse =
        responses.countByMeetingIdAndResponse(meetingId, MeetingResponse.ATTENDING).let { attending ->
            if (attending > 0) MeetingResponse.ATTENDING else MeetingResponse.PENDING
        }

}
