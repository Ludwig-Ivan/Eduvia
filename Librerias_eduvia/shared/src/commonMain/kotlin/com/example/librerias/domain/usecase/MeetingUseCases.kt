package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.domain.repository.MeetingRepository
import com.example.librerias.domain.util.TodayProvider
import com.example.librerias.domain.validation.Validators
import com.example.librerias.model.AppResult
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.MEETING_ID_OFFSET
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.NewMeetingInput
import com.example.librerias.model.isBefore
import com.example.librerias.model.parseDateOrNull
import com.example.librerias.model.relativeLabel
import com.example.librerias.model.toEpochDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first // 👈 IMPORTANTE
import kotlinx.coroutines.flow.map

/**
 * Lista ordenada por fecha; marca la próxima junta (isNext + "En 3 días").
 *
 * La regla de "próxima" depende de `today()`, por eso se inyecta [TodayProvider] en vez de
 * leer la fecha de la plataforma: la UI siempre resalta la primera junta que aún no pasó.
 *
 * @param meetings repositorio de juntas.
 * @param today proveedor de la fecha actual para calcular `isNext` y la etiqueta relativa.
 * @return flujo de juntas ordenadas, con una sola marcada como próxima.
 */
class ObserveMeetingsUseCase(
    private val meetings: MeetingRepository,
    private val today: TodayProvider
) {
    operator fun invoke(): Flow<List<Meeting>> = meetings.observeMeetings().map { list ->
        val now = today.today()
        val sorted = list.sortedBy { it.date.toEpochDay() }
        val nextId = sorted.firstOrNull { !it.date.isBefore(now) }?.id
        sorted.map { m ->
            if (m.id == nextId) m.copy(isNext = true, relativeLabel = m.date.relativeLabel(now))
            else m.copy(isNext = false, relativeLabel = null)
        }
    }
}

/**
 * Registra la respuesta de un alumno en una junta. La asistencia no se guarda como entidad
 * independiente: se deriva de estas respuestas, por lo que al escribirla se recalcula sola.
 *
 * @param meetings repositorio de juntas y respuestas.
 * @return sin valor; los observadores de asistencia y respuestas reciben el cambio.
 */
class RespondToMeetingUseCase(private val meetings: MeetingRepository) {
    /**
     * @param meetingId junta a la que responde la familia.
     * @param studentId alumno cuya respuesta se registra.
     * @param response respuesta (Confirmada / Rechazada / Pendiente).
     * @return sin valor.
     */
    suspend operator fun invoke(meetingId: Long, studentId: Long, response: MeetingResponse) {
        meetings.setResponse(meetingId, studentId, response)
    }
}

/**
 * Devuelve las respuestas de un alumno indexadas por junta, no una lista plana: así la pantalla
 * consulta en O(1) qué respondió en cada junta sin filtrar repetidamente.
 *
 * @param meetings repositorio de juntas y respuestas.
 * @return flujo con el mapa `meetingId -> MeetingResponse` de ese alumno.
 */
class ObserveStudentResponsesUseCase(private val meetings: MeetingRepository) {
    /**
     * @param studentId id del alumno cuyas respuestas se observan.
     * @return flujo del mapa de respuestas por junta.
     */
    operator fun invoke(studentId: Long): Flow<Map<Long, MeetingResponse>> =
        meetings.observeResponses(studentId)
}

/**
 * Asistencia derivada: para cada alumno del grupo de la junta se busca su respuesta y, si no
 * existe, se muestra `PENDING`. No hay una tabla de asistencia que mantener a mano.
 *
 * @param meetings repositorio de juntas y respuestas.
 * @return flujo con una `FamilyResponse` por alumno del grupo.
 */
class ObserveAttendanceUseCase(private val meetings: MeetingRepository) {
    /**
     * @param meetingId junta cuya asistencia se observa.
     * @return flujo de respuestas por familia, con `PENDING` para quien aún no responde.
     */
    operator fun invoke(meetingId: Long): Flow<List<FamilyResponse>> =
        meetings.observeAttendance(meetingId)
}

/**
 * Elimina la junta y sus respuestas; el repositorio remoto también retira su evento de calendario
 * (`MEETING_ID_OFFSET + id`) para que no quede huérfano.
 *
 * @param meetings repositorio de juntas.
 * @return sin valor.
 */
class DeleteMeetingUseCase(private val meetings: MeetingRepository) {
    /**
     * @param meetingId id de la junta a borrar.
     * @return sin valor.
     */
    suspend operator fun invoke(meetingId: Long) = meetings.deleteMeeting(meetingId)
}

/**
 * Los recordatorios se calculan sobre la asistencia derivada: se cuentan las familias cuyas
 * respuestas siguen `PENDING` y el backend devuelve cuántas se avisaron.
 *
 * @param meetings repositorio de juntas y respuestas.
 * @return número de familias pendientes de responder.
 */
class RemindPendingFamiliesUseCase(private val meetings: MeetingRepository) {
    /**
     * @param meetingId junta cuyos recordatorios se envían.
     * @return cantidad de respuestas pendientes (0 si ya respondieron todas).
     */
    suspend operator fun invoke(meetingId: Long): Int = meetings.remindPending(meetingId)
}

/**
 * Crea una junta aplicando validación por pasos: título, fecha no anterior a hoy, hora futura
 * si es hoy, lugar, enlace http(s) opcional y, por último, unicidad de juntas (no puede haber
 * otra igual en fecha, hora, lugar y grupo). Solo después de pasar todo se guarda y se crea su
 * evento de calendario con id `MEETING_ID_OFFSET + id` (`1000 + id`).
 *
 * @param meetings repositorio donde se guarda la junta.
 * @param calendar repositorio donde se registra el evento de calendario.
 * @param today proveedor de fecha y hora para las comprobaciones temporales.
 * @return la junta guardada, o `AppResult.Error` con el primer error de validación o duplicado.
 */
class CreateMeetingUseCase(
    private val meetings: MeetingRepository,
    private val calendar: CalendarRepository,
    private val today: TodayProvider
) {
    /**
     * @param input datos del formulario de nueva junta, incluido el grupo al que pertenece.
     * @return la junta creada con su id real, o el error correspondiente.
     */
    suspend operator fun invoke(input: NewMeetingInput): AppResult<Meeting> {
        val now = today.today()

        if (input.title.isBlank()) return AppResult.Error("Escribe el título de la junta.")
        val date = parseDateOrNull(input.dateText, now.year)
            ?: return AppResult.Error("La fecha no es válida. Usa el formato DD/MM/AAAA, por ejemplo 19/11/2026.")
        if (date.isBefore(now)) return AppResult.Error("La fecha no puede ser anterior a hoy.")
        val minutes = Validators.timeToMinutes(input.time)
            ?: return AppResult.Error("La hora no es válida. Ejemplo: 8:00 am.")

// Si la junta es hoy, la hora no puede haber pasado
        if (date == now && minutes <= today.nowMinutes())
            return AppResult.Error("Esa hora ya pasó. Elige una hora posterior a la actual.")
        if (input.place.isBlank()) return AppResult.Error("Escribe el lugar de la junta.")

        val link = input.link.trim()
        if (link.isNotEmpty() && !Validators.isValidUrl(link))
            return AppResult.Error("El enlace debe empezar con http:// o https://")

        val currentMeetings = meetings.observeMeetings().first()

        val existingMeeting = currentMeetings.firstOrNull { existing ->
            existing.date == date &&
                    existing.time.trim().equals(input.time.trim(), ignoreCase = true) &&
                    existing.location.trim().equals(input.place.trim(), ignoreCase = true) &&
                    existing.groupId == input.groupId
        }

        if (existingMeeting != null) {
            return AppResult.Error("Ya existe una junta en esa fecha, hora y lugar para este grupo.")
        }

        // 👆 FIN DE LA VALIDACIÓN

        val saved = meetings.addMeeting(
            Meeting(
                id = 0,
                groupId = input.groupId,
                title = input.title.trim(),
                date = date,
                time = input.time.trim(),
                location = input.place.trim(),
                hasVirtualLink = link.isNotEmpty(),
                virtualLink = link.ifEmpty { null },
                reminderEnabled = input.reminder
            )
        )
        calendar.addEvent(
            CalendarEvent(
                id = MEETING_ID_OFFSET + saved.id,
                groupId = saved.groupId,
                date = date,
                type = CalendarEventType.MEETING,
                title = saved.title,
                subtitle = "${saved.time} · ${saved.location}"
            )
        )
        return AppResult.Success(saved)
    }
}

/**
 * Edición real: conserva el id y excluye la propia junta de la validación de duplicados.
 *
 * Conserva el id de [original] y, en la comprobación de unicidad de juntas,
 * excluye la propia junta (si no, siempre se encontraría a sí misma como duplicado). Se permite
 * dejar la fecha original aunque ya pasó cuando solo se edita el texto. Después se actualiza
 * también el evento de calendario `MEETING_ID_OFFSET + id` (`1000 + id`).
 *
 * @param meetings repositorio donde se actualiza la junta.
 * @param calendar repositorio donde se actualiza el evento de calendario.
 * @param today proveedor de fecha y hora para las comprobaciones temporales.
 * @return la junta actualizada, o `AppResult.Error` si falla alguna validación o hay duplicado.
 */
class UpdateMeetingUseCase(
    private val meetings: MeetingRepository,
    private val calendar: CalendarRepository,
    private val today: TodayProvider
) {
    /**
     * @param original junta que se edita; su id se conserva.
     * @param title nuevo título.
     * @param dateText nueva fecha en texto; se acepta la original pasada si no cambia.
     * @param time nueva hora, no puede ser pasada si la junta es hoy y cambia.
     * @param place nuevo lugar.
     * @param link nuevo enlace virtual opcional (http:// o https://).
     * @param reminder si el recordatorio sigue activo.
     * @return la junta guardada, o el error correspondiente.
     */
    suspend operator fun invoke(
        original: Meeting,
        title: String,
        dateText: String,
        time: String,
        place: String,
        link: String,
        reminder: Boolean,
    ): AppResult<Meeting> {
        val now = today.today()
        if (title.isBlank()) return AppResult.Error("Escribe el título de la junta.")
        val date = parseDateOrNull(dateText, now.year)
            ?: return AppResult.Error("La fecha no es válida. Usa el formato DD/MM/AAAA, por ejemplo 19/11/2026.")
        if (date != original.date && date.isBefore(now))
            return AppResult.Error("La fecha no puede ser anterior a hoy.")
        val minutes = Validators.timeToMinutes(time)
            ?: return AppResult.Error("La hora no es válida. Ejemplo: 8:00 am.")
        val changesTime = !time.trim().equals(original.time.trim(), ignoreCase = true)
        if (date == now && changesTime && minutes <= today.nowMinutes())
            return AppResult.Error("Esa hora ya pasó. Elige una hora posterior a la actual.")
        if (place.isBlank()) return AppResult.Error("Escribe el lugar de la junta.")

        val normalizedLink = link.trim()
        if (normalizedLink.isNotEmpty() && !Validators.isValidUrl(normalizedLink))
            return AppResult.Error("El enlace debe empezar con http:// o https://")

        val duplicate = meetings.observeMeetings().first().any { existing ->
            existing.id != original.id &&
                existing.date == date &&
                existing.time.trim().equals(time.trim(), ignoreCase = true) &&
                existing.location.trim().equals(place.trim(), ignoreCase = true) &&
                existing.groupId == original.groupId
        }
        if (duplicate) {
            return AppResult.Error("Ya existe una junta en esa fecha, hora y lugar para este grupo.")
        }

        val updated = original.copy(
            title = title.trim(),
            date = date,
            time = time.trim(),
            location = place.trim(),
            virtualLink = normalizedLink.ifEmpty { null },
            hasVirtualLink = normalizedLink.isNotEmpty(),
            reminderEnabled = reminder,
        )
        val saved = meetings.updateMeeting(updated)
        calendar.addEvent(
            CalendarEvent(
                id = MEETING_ID_OFFSET + saved.id,
                groupId = saved.groupId,
                date = saved.date,
                type = CalendarEventType.MEETING,
                title = saved.title,
                subtitle = "${saved.time} · ${saved.location}",
            )
        )
        return AppResult.Success(saved)
    }
}
