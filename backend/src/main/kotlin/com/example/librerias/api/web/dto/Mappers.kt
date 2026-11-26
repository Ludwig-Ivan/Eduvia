package com.example.librerias.api.web.dto

import com.example.librerias.api.domain.CalendarEvent
import com.example.librerias.api.domain.Meeting
import com.example.librerias.api.domain.MeetingResponse
import com.example.librerias.api.domain.SchoolGroup
import com.example.librerias.api.domain.SchoolTask
import com.example.librerias.api.domain.Student
import com.example.librerias.api.domain.User
import java.time.LocalDate

/**
 * Capa: mapper (entity -> dto). Convierte un `User` de `users` en [UserDto] para `GET /me` y
 * para el `user` de la respuesta de registro/login.
 *
 * Traducción de contrato: copia el `role` con el mismo nombre del enum (`PARENT`/`TEACHER`),
 * que `kotlinx.serialization` espera en la app; `passwordHash` y la auditoría nunca se exponen.
 */
fun User.toDto() = UserDto(id, fullName, email, phone, role)

/**
 * Capa: mapper (entity -> dto). Convierte un `SchoolGroup` de `school_groups` en
 * [SchoolGroupDto] para los endpoints `/groups` y para `createdGroups` del registro.
 *
 * `studentCount` se traslada tal cual: es el contador cacheado que `GroupService.linkStudent`
 * incrementa, no un recuento calculado aquí. El `code` de invitación viaja sin normalizar.
 */
fun SchoolGroup.toDto() = SchoolGroupDto(id, name, school, subject, code, teacherId, studentCount)

/**
 * La app usa parentId = 0 para "sin tutor"; la BD guarda NULL.
 *
 * Capa: mapper (entity -> dto). Traduce un `Student` de `students` a [StudentDto] para
 * `GET /groups/{id}/students`, `GET /students` y `POST /students:link`. El único ajuste de
 * contrato es `parentId ?: 0L`: `NULL` en la BD se convierte en el centinela `0` que la app
 * interpreta como "sin tutor" (y que a la inversa recibe `LinkStudentRequest.parentId`).
 */
fun Student.toDto() = StudentDto(id, name, groupId, parentId ?: 0L)

/**
 * Capa: mapper (entity -> dto). Convierte un `SchoolTask` de `school_tasks` en [SchoolTaskDto]
 * para los endpoints de tareas (`GET`, `POST`, `PUT` y marcas de entrega).
 *
 * Traducciones de contrato: `subject` viaja como el nombre del enum (`MATEMATICAS`, `ESPANOL`,
 * ...); `dueDate` y `publishedDate` son `LocalDate` y @JsonFormat en el DTO los serializa como
 * `YYYY-MM-DD`, igual que el `dateText` que envía la app. `deliveredIds` (obtenidos por
 * `SchoolTaskRepository.findDeliveredStudentIds`) se convierten en `deliveredStudentIds`, y el
 * `status` es un valor **derivado** por `TaskService` (`DELIVERED`/`EXPIRED`/`PENDING`): con
 * valor `null` por defecto en los listados. `hasPhoto` se copia como bandera heredada de la app.
 */
fun SchoolTask.toDto(
    deliveredIds: List<Long>,
    status: String? = null,
) = SchoolTaskDto(
    id = id,
    groupId = groupId,
    title = title,
    subject = subject,
    description = description,
    dueDate = dueDate,
    publishedDate = publishedDate,
    teacherName = teacherName,
    totalStudents = totalStudents,
    hasPhoto = hasPhoto,
    deliveredStudentIds = deliveredIds,
    status = status,
)

/**
 * Capa: mapper (entity -> dto). Convierte un `Meeting` de `meetings` en [MeetingDto] para
 * `GET /meetings`, `GET /meetings/{id}` y las respuestas de creación/edición/respuesta.
 *
 * Traducciones y campos derivados del contrato de la app:
 * - `date` se serializa como `YYYY-MM-DD` gracias a @JsonFormat en el DTO; `time` se mantiene
 *   como texto (p. ej. `"8:00 am"`), tal como lo envía la app.
 * - `hasVirtualLink` se deriva de `virtualLink != null` (no se fía solo del flag guardado).
 * - `isNext` es `fecha >= hoy` (`!date.isBefore(today)`): la junta de hoy cuenta como próxima.
 *   El parámetro `today` es inyectable para hacer deterministas las pruebas.
 * - `relativeLabel` se calcula con [relativeLabel]: `null` si ya pasó, `"Hoy"`, `"Mañana"` o
 *   `"En N días"`. La app original lo calculaba en `ObserveMeetingsUseCase`.
 * - `response` es **derivado**: por defecto `PENDING` y `MeetingService` lo cambia a
 *   `ATTENDING` cuando existe alguna respuesta de alumno en ese sentido.
 */
fun Meeting.toDto(
    today: LocalDate = LocalDate.now(),
    response: MeetingResponse = MeetingResponse.PENDING,
): MeetingDto {
    val isNext = !date.isBefore(today)
    return MeetingDto(
        id = id,
        groupId = groupId,
        title = title,
        date = date,
        time = time,
        location = location,
        hasVirtualLink = virtualLink != null,
        isNext = isNext,
        relativeLabel = relativeLabel(date, today),
        response = response,
        virtualLink = virtualLink,
        reminderEnabled = reminderEnabled,
    )
}

/**
 * Capa: mapper (ayudado de [Meeting.toDto]). Etiqueta relativa en español que la app mostraba
 * calculada en el cliente; el backend la deriva para no cambiar el contrato JSON.
 *
 * Traducción: diferencia en días entre `today` y `date`; fechas pasadas devuelven `null`,
 * hoy `"Hoy"`, mañana `"Mañana"` y el resto `"En $days días"`. Junto con `isNext`, reproduce
 * el comportamiento de `ObserveMeetingsUseCase` de la app original.
 */
private fun relativeLabel(date: LocalDate, today: LocalDate): String? {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> null
        days == 0L -> "Hoy"
        days == 1L -> "Mañana"
        else -> "En $days días"
    }
}

/**
 * Capa: mapper (entity -> dto). Convierte un `CalendarEvent` de `calendar_events` en
 * [CalendarEventDto] para `GET /calendar/events` y `POST /calendar/events`.
 *
 * Traducciones: `date` se serializa como `YYYY-MM-DD` y `type` viaja como el nombre del enum
 * (`TASK`, `MEETING`, `EXAM`). El `id` se copia tal cual: ya viene desplazado con
 * `MEETING_ID_OFFSET` para las juntas y coincide con el id de la tarea en los eventos `TASK`.
 *
 * `attendance` es un campo **derivado**, no una columna: [attendanceSummary] lo calcula
 * `CalendarService` solo para juntas y solo en la vista del docente (`role=TEACHER`); con
 * `null` por defecto la vista del padre no recibe información de asistencia.
 */
fun CalendarEvent.toDto(attendance: AttendanceSummaryDto? = null) = CalendarEventDto(
    id = id,
    groupId = groupId,
    date = date,
    type = type,
    title = title,
    subtitle = subtitle,
    attendance = attendance,
)

/**
 * Capa: mapper (fábrica de dto). Construye [AttendanceSummaryDto] a partir de los tres contadores
 * de `meeting_responses` agrupados por respuesta (`ATTENDING`, `DECLINED`, `PENDING`).
 *
 * Es un valor derivado que la app calculaba en memoria (`AttendanceSummary.total` y
 * `confirmedFraction`); el backend lo rellena en el calendario del docente sin persistirlo.
 */
fun attendanceSummary(confirmed: Int, declined: Int, pending: Int) =
    AttendanceSummaryDto(confirmed, declined, pending)
