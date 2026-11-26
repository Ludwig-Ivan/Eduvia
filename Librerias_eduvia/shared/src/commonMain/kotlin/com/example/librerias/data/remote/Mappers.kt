package com.example.librerias.data.remote

import com.example.librerias.model.*

/**
 * Traducción entre DTOs de red y modelos de dominio de la app.
 * Reglas del contrato: enums como literales en mayúsculas, fechas `CalendarDate` ↔ String ISO
 * `YYYY-MM-DD` y `parentId = 0` como centinela de «sin tutor» (NULL en la BD del backend, que
 * convierte `null ↔ 0`). Los campos derivados (`status`, `isNext`, `relativeLabel`, asistencia) los
 * envía el backend como conveniencia, pero el dominio puede recalcularlos. Los eventos de junta se
 * identifican en la API con id `1000 + meetings.id` (`MEETING_ID_OFFSET`).
 */

/** Convierte una fecha ISO `YYYY-MM-DD` del DTO en [CalendarDate]; exige tres componentes. */
internal fun String.toCalendarDate(): CalendarDate {
    val parts = split('-')
    require(parts.size == 3) { "Fecha inválida: $this" }
    return CalendarDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

/** Serializa [CalendarDate] como `YYYY-MM-DD`: año de 4 dígitos, mes y día de 2. */
internal fun CalendarDate.toApiDate(): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

/** Parsea el literal del DTO al enum [E] por su nombre exacto; usa [default] si no se reconoce. */
private inline fun <reified E : Enum<E>> String.asEnum(default: E): E =
    runCatching { enumValueOf<E>(this) }.getOrDefault(default)

/** Literal `PARENT`/`TEACHER` → [UserRole]; si no se reconoce, [UserRole.PARENT]. */
internal fun String.toUserRole(): UserRole = asEnum(UserRole.PARENT)

/** Literal de materia → [Subject]; si no se reconoce, [Subject.OTRA]. */
internal fun String.toSubject(): Subject = asEnum(Subject.OTRA)

/** Literal `PENDING`/`ATTENDING`/`DECLINED` → [MeetingResponse]; por defecto PENDING. */
internal fun String.toMeetingResponse(): MeetingResponse = asEnum(MeetingResponse.PENDING)

/** Literal `TASK`/`MEETING`/`EXAM` → [CalendarEventType]; por defecto EXAM. */
internal fun String.toCalendarEventType(): CalendarEventType = asEnum(CalendarEventType.EXAM)

/** Respuesta A1/A2/A3/A9 → [User]: `role` se convierte por literal y los demás campos se copian. */
internal fun UserDto.toModel() = User(id, fullName, email, phone, role.toUserRole())

/** Respuesta G1/G2/G3/G4 → [SchoolGroup]: copia directa de los campos del DTO. */
internal fun GroupDto.toModel() = SchoolGroup(id, name, school, subject, code, teacherId, studentCount)

/**
 * Respuesta G5/G6/G7 → [Student]: `parentId = 0` es el centinela de «sin tutor asignado»; el
 * backend traduce ese valor a `NULL` en la tabla y de vuelta a `0` en la respuesta.
 */
internal fun StudentDto.toModel() = Student(id, name, groupId, parentId)

/**
 * Respuesta T1/T2/T3/T4/T6 → [SchoolTask]: materia por literal, fechas ISO a [CalendarDate] y
 * entregas de lista a conjunto. Campos derivados como `teacherName`, `totalStudents`, `hasPhoto` y
 * `deliveredStudentIds` se copian tal cual; `status` no se traslada porque el dominio calcula el
 * estado con `statusFor(today, studentId)`.
 */
internal fun TaskDto.toModel() = SchoolTask(
    id = id,
    groupId = groupId,
    title = title,
    subject = subject.asEnum(Subject.OTRA),
    description = description,
    dueDate = dueDate.toCalendarDate(),
    publishedDate = publishedDate.toCalendarDate(),
    teacherName = teacherName,
    totalStudents = totalStudents,
    hasPhoto = hasPhoto,
    deliveredStudentIds = deliveredStudentIds.toSet(),
)

/**
 * Respuesta M1/M2/M3/M6/M9 → [Meeting]: `date` ISO a [CalendarDate] y `response` por literal.
 * `isNext` y `relativeLabel` son derivados: se trasladan tal cual y el caso de uso de juntas puede
 * recalcularlos. El evento de calendario de una junta no usa su id, sino `1000 + meetings.id`
 * (`MEETING_ID_OFFSET`), convención aplicada fuera de este mapper.
 */
internal fun MeetingDto.toModel() = Meeting(
    id = id,
    groupId = groupId,
    title = title,
    date = date.toCalendarDate(),
    time = time,
    location = location,
    hasVirtualLink = hasVirtualLink,
    isNext = isNext,
    relativeLabel = relativeLabel,
    response = response.asEnum(MeetingResponse.PENDING),
    virtualLink = virtualLink,
    reminderEnabled = reminderEnabled,
)

/** Respuesta M5 → [FamilyResponse]: `response` se convierte por literal y se conserva `studentId`. */
internal fun FamilyResponseDto.toModel() = FamilyResponse(familyName, response.asEnum(MeetingResponse.PENDING), studentId)

/**
 * Resumen C1 → [AttendanceSummary]: recuento derivado de las respuestas de las familias; `total` y
 * `confirmedFraction` se calculan como propiedades del modelo, no se envían en el DTO.
 */
internal fun AttendanceDto.toModel() = AttendanceSummary(confirmed, declined, pending)

/**
 * Respuesta C1/C2 → [CalendarEvent]: `type` por literal, fecha ISO a [CalendarDate] y asistencia
 * opcional (ausente para el rol PARENT). Los eventos TASK y MEETING los crea el backend junto a la
 * tarea o junta; en MEETING su id sigue la convención `1000 + meetings.id` (`MEETING_ID_OFFSET`).
 */
internal fun CalendarEventDto.toModel() = CalendarEvent(
    id = id,
    groupId = groupId,
    date = date.toCalendarDate(),
    type = type.asEnum(CalendarEventType.EXAM),
    title = title,
    subtitle = subtitle,
    attendance = attendance?.toModel(),
)

/** [SchoolTask] → request T3: `subject` como literal y `dueDate` como `YYYY-MM-DD` en `dateText`. */
internal fun SchoolTask.toCreateRequest() = CreateTaskRequestDto(
    groupId = groupId,
    title = title,
    subject = subject.name,
    description = description,
    dateText = dueDate.toApiDate(),
)

/** [SchoolTask] → request T4: mismas reglas que `toCreateRequest`, sin incluir `groupId`. */
internal fun SchoolTask.toUpdateRequest() = UpdateTaskRequestDto(
    title = title,
    subject = subject.name,
    description = description,
    dateText = dueDate.toApiDate(),
)

/**
 * [Meeting] → request M3: fecha como `YYYY-MM-DD`, `location` → `place`, `virtualLink` → `link` y
 * `reminderEnabled` → `reminder`. El id del evento de calendario (`1000 + meetings.id`) no viaja
 * en el request: lo asigna el backend.
 */
internal fun Meeting.toCreateRequest() = CreateMeetingRequestDto(
    title = title,
    dateText = date.toApiDate(),
    time = time,
    place = location,
    link = virtualLink,
    reminder = reminderEnabled,
    groupId = groupId,
)

/** [Meeting] → request M9: mismas reglas que `toCreateRequest`, sin incluir `groupId`. */
internal fun Meeting.toUpdateRequest() = UpdateMeetingRequestDto(
    title = title,
    dateText = date.toApiDate(),
    time = time,
    place = location,
    link = virtualLink,
    reminder = reminderEnabled,
)

/**
 * [CalendarEvent] → request C2: `type` como literal y fecha ISO `YYYY-MM-DD`. El id no se envía;
 * para eventos MEETING la API usa `1000 + meetings.id` (`MEETING_ID_OFFSET`).
 */
internal fun CalendarEvent.toCreateRequest() = CreateCalendarEventRequestDto(
    groupId = groupId,
    date = date.toApiDate(),
    type = type.name,
    title = title,
    subtitle = subtitle,
)
