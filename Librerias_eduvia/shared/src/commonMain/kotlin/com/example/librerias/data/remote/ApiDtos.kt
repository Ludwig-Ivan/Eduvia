package com.example.librerias.data.remote

import kotlinx.serialization.Serializable

/**
 * DTOs de la API REST. Se mantienen separados de los modelos de dominio de la app.
 * Convención del contrato: las fechas de calendario viajan como `String` `YYYY-MM-DD` (CalendarDate
 * en dominio), los ids son `Long` y los enums se envían como literales en mayúsculas. Los DTOs
 * terminados en `RequestDto` son payloads enviados al backend; los demás son respuestas.
 */

/** Respuesta (A1/A2/A3, y A9 en `/me`): usuario de registro, login, refresh o perfil. */
@Serializable
data class UserDto(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
)

/** Respuesta (G1/G2/G3/G4 y `createdGroups` de A1): grupo escolar con código y número de alumnos. */
@Serializable
data class GroupDto(
    val id: Long,
    val name: String,
    val school: String,
    val subject: String,
    val code: String,
    val teacherId: Long,
    val studentCount: Int,
)

/** Respuesta (G5/G6/G7): alumno con su grupo y tutor (`parentId = 0` significa sin tutor). */
@Serializable
data class StudentDto(
    val id: Long,
    val name: String,
    val groupId: Long,
    val parentId: Long,
)

/** Respuesta (T1/T2/T3/T4/T6): tarea; `status` es derivado y las fechas usan `YYYY-MM-DD`. */
@Serializable
data class TaskDto(
    val id: Long,
    val groupId: Long,
    val title: String,
    val subject: String,
    val description: String,
    val dueDate: String,
    val publishedDate: String,
    val teacherName: String,
    val totalStudents: Int,
    val hasPhoto: Boolean = false,
    val deliveredStudentIds: List<Long> = emptyList(),
    val status: String? = null,
)

/** Respuesta (M1/M2/M3/M6/M9): junta; `date` en `YYYY-MM-DD`, `isNext`/`relativeLabel` derivados. */
@Serializable
data class MeetingDto(
    val id: Long,
    val groupId: Long,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val hasVirtualLink: Boolean = false,
    val isNext: Boolean = false,
    val relativeLabel: String? = null,
    val response: String = "PENDING",
    val virtualLink: String? = null,
    val reminderEnabled: Boolean = true,
)

/** Respuesta (M5): respuesta de una familia a una junta, identificada por `studentId`. */
@Serializable
data class FamilyResponseDto(
    val familyName: String,
    val response: String,
    val studentId: Long,
)

/** Respuesta embebida (C1, dentro de `CalendarEventDto`): resumen derivado de la asistencia. */
@Serializable
data class AttendanceDto(
    val confirmed: Int,
    val declined: Int,
    val pending: Int,
)

/** Respuesta (C1/C2): evento de calendario; `date` en `YYYY-MM-DD` y `attendance` solo para docentes. */
@Serializable
data class CalendarEventDto(
    val id: Long,
    val groupId: Long,
    val date: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val attendance: AttendanceDto? = null,
)

/**
 * Respuesta paginada (G1/G5/G6, T1, M1/M5, C1): `items` son los elementos de la página actual,
 * `page` y `size` describen esa página y `totalElements`/`totalPages` el total disponible.
 */
@Serializable
data class PageDto<T : Any>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** Respuesta (A1/A2/A3): tokens de sesión, usuario y grupos creados durante el registro. */
@Serializable
data class TokenResponseDto(
    val user: UserDto? = null,
    val createdGroups: List<GroupDto> = emptyList(),
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

/** Respuesta (A5/A6): indica si el email o el teléfono ya está registrado. */
@Serializable
data class ExistsDto(val exists: Boolean)

/** Respuesta (A7, solo en `dev`): código de reset; se omite en producción. */
@Serializable
data class ResetCodeDto(val resetCode: String? = null)

/** Respuesta (M7): mapa de id de junta (como texto) a la respuesta de la familia. */
@Serializable
data class ResponsesDto(val responses: Map<String, String>)

/** Respuesta (M8): número de respuestas pendientes de una junta. */
@Serializable
data class PendingDto(val pending: Int)

/** Parte de [ProblemDto]: error de validación de un campo (`field` y `message`). */
@Serializable
data class FieldErrorDto(val field: String, val message: String)

/** Respuesta de error (RFC 9457) en cualquier endpoint: alimenta el mensaje de [ApiException]. */
@Serializable
data class ProblemDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val errors: List<FieldErrorDto> = emptyList(),
)

/** Request (A2): credenciales enviadas a `POST auth/login`. */
@Serializable
data class LoginRequestDto(val email: String, val password: String)

/** Request (A1): alta de usuario con hijos y grupos opcionales. */
@Serializable
data class RegisterRequestDto(
    val role: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val acceptedTerms: Boolean = true,
    val children: List<ChildDto> = emptyList(),
    val groups: List<GroupInputDto> = emptyList(),
)

/** Request anidado (A1, campo `children`): alumno a vincular mediante código de clase. */
@Serializable
data class ChildDto(val classCode: String, val studentName: String)

/** Request anidado (A1, campo `groups`): grupo a crear durante el registro docente. */
@Serializable
data class GroupInputDto(val groupName: String, val schoolName: String, val subject: String)

/** Request (A3/A4): refresh token usado para renovar la sesión o revocarla en logout. */
@Serializable
data class RefreshRequestDto(val refreshToken: String)

/** Request (A7): email al que se envía el código de restablecimiento. */
@Serializable
data class ResetCodeRequestDto(val email: String)

/** Request (A8): email, código recibido y nueva contraseña (repetida como confirmación). */
@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    val newPassword: String,
    val confirmPassword: String,
)

/** Request (G4): datos necesarios para crear un grupo docente. */
@Serializable
data class CreateGroupRequestDto(
    val teacherId: Long,
    val groupName: String,
    val schoolName: String,
    val subject: String,
)

/** Request (G7): vincula un alumno con un tutor mediante el código de clase. */
@Serializable
data class LinkStudentRequestDto(
    val parentId: Long,
    val classCode: String,
    val studentName: String,
)

/** Request (T3): crea una tarea; `dateText` viaja como fecha ISO `YYYY-MM-DD`. */
@Serializable
data class CreateTaskRequestDto(
    val groupId: Long,
    val title: String,
    val subject: String,
    val description: String,
    val dateText: String,
)

/** Request (T4): edita una tarea; `dateText` viaja como fecha ISO `YYYY-MM-DD`. */
@Serializable
data class UpdateTaskRequestDto(
    val title: String,
    val subject: String,
    val description: String,
    val dateText: String,
)

/** Request (T6): marca una tarea como entregada por un alumno. */
@Serializable
data class MarkDeliveredRequestDto(val studentId: Long)

/** Request (M3): crea una junta; `dateText` en `YYYY-MM-DD` y `link` opcional. */
@Serializable
data class CreateMeetingRequestDto(
    val title: String,
    val dateText: String,
    val time: String,
    val place: String,
    val link: String? = null,
    val reminder: Boolean = true,
    val groupId: Long,
)

/** Request (M9): edita una junta; `dateText` en `YYYY-MM-DD` y `link` opcional. */
@Serializable
data class UpdateMeetingRequestDto(
    val title: String,
    val dateText: String,
    val time: String,
    val place: String,
    val link: String? = null,
    val reminder: Boolean = true,
)

/** Request (M6): respuesta del alumno a una junta (`PENDING`, `ATTENDING` o `DECLINED`). */
@Serializable
data class SetResponseRequestDto(val response: String)

/** Request (C2): crea un evento EXAM; `date` en `YYYY-MM-DD` y `subtitle` opcional. */
@Serializable
data class CreateCalendarEventRequestDto(
    val groupId: Long,
    val date: String,
    val type: String,
    val title: String,
    val subtitle: String = "",
)
