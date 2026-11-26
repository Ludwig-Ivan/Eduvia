package com.example.librerias.api.web.dto

import com.example.librerias.api.domain.CalendarEventType
import com.example.librerias.api.domain.MeetingResponse
import com.example.librerias.api.domain.Subject
import com.example.librerias.api.domain.UserRole
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * Capa: dto (respuesta). Contrato de paginación de la app para todos los listados.
 *
 * Envuelve `items` con la ventana devuelta y los metadatos que la app necesita para avanzar:
 * `page` es el índice **empezando en 0**, `size` el tamaño solicitado (el controlador lo limita
 * a 100), `totalElements` el total en BD y `totalPages` el número de páginas. `PageResponse.of`
 * adapta un `org.springframework.data.domain.Page` sin alterar esos valores.
 *
 * Colaboradores: todos los controladores de listado (grupos, alumnos, tareas, juntas,
 * asistencia y calendario); `MeetingService` y `CalendarService` lo construyen a mano cuando
 * paginan en memoria. Ejemplo: `{ "items": [], "page": 0, "size": 50, "totalElements": 0,
 * "totalPages": 0 }`.
 */
data class PageResponse<T : Any>(
    @field:Schema(description = "Elementos de la página actual", example = "[]")
    val items: List<T>,
    @field:Schema(description = "Índice de página, empieza en 0", example = "0")
    val page: Int,
    @field:Schema(description = "Tamaño de página solicitado (máximo 100)", example = "50")
    val size: Int,
    @field:Schema(description = "Número total de elementos", example = "12")
    val totalElements: Long,
    @field:Schema(description = "Número total de páginas", example = "1")
    val totalPages: Int,
) {
    companion object {
        fun <T : Any> of(page: org.springframework.data.domain.Page<T>): PageResponse<T> = PageResponse(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}

/**
 * Capa: dto (respuesta). Usuario devuelto por `GET /api/v1/me` y anidado en [TokenResponse]
 * en login/registro.
 *
 * No expone `passwordHash` ni datos de auditoría. El `role` viaja como `PARENT`/`TEACHER`,
 * igual que en el enum de la app. Ejemplo: `{"id":7,"fullName":"Ana Pérez",
 * "email":"ana@example.com","phone":"612345678","role":"PARENT"}`.
 */
data class UserDto(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    @field:Schema(description = "Perfil del usuario: PARENT o TEACHER", example = "PARENT")
    val role: UserRole,
)

/**
 * Capa: dto (parte de una request). Hijos que se envían dentro de [RegisterRequest] en
 * `POST /api/v1/auth/register`.
 *
 * Validación: `classCode` y `studentName` no pueden estar en blanco (`@NotBlank`). El código
 * de clase tiene el formato `AA-1234` que genera el backend. La app actual envía esta lista
 * vacía y vincula los hijos después con `POST /students:link`.
 */
@Schema(description = "Hijo a registrar junto con el padre (actualmente se envía vacío).")
data class ChildInfoDto(
    @field:NotBlank @field:Schema(description = "Código de la clase, p. ej. AB-1234", example = "AB-1234")
    val classCode: String,
    @field:NotBlank @field:Schema(description = "Nombre del alumno", example = "Lucía Pérez")
    val studentName: String,
)

/**
 * Capa: dto (parte de una request). Grupos que un docente envía dentro de [RegisterRequest]
 * en `POST /api/v1/auth/register`.
 *
 * Validación: `groupName`, `schoolName` y `subject` son obligatorios (`@NotBlank`). La app
 * actual los envía vacíos y crea los grupos después con `POST /groups`.
 */
@Schema(description = "Grupo a crear durante el registro del docente (actualmente se envía vacío).")
data class GroupInputDto(
    @field:NotBlank @field:Schema(description = "Nombre del grupo", example = "3º A")
    val groupName: String,
    @field:NotBlank @field:Schema(description = "Nombre del colegio", example = "Colegio San Juan")
    val schoolName: String,
    @field:NotBlank @field:Schema(description = "Materia del grupo", example = "Matemáticas")
    val subject: String,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/auth/register`.
 *
 * Validación: `role` obligatorio (`@NotNull`), `fullName`/`phone` no vacíos, `email` con formato
 * y no vacío, `password` de al menos 6 caracteres, `confirmPassword` no vacío y `acceptedTerms`
 * debe ser `true` (`@AssertTrue`, mensaje "Debes aceptar los términos"). `children` y `groups`
 * son opcionales y por ahora llegan vacíos (decisión 10 de `DECISIONS.md`).
 *
 * Respuesta: [TokenResponse] con usuario, grupos creados y tokens.
 */
@Schema(description = "Registro de padre o docente; devuelve tokens JWT.")
data class RegisterRequest(
    @field:NotNull @field:Schema(description = "PARENT o TEACHER", example = "PARENT")
    val role: UserRole,
    @field:NotBlank @field:Schema(description = "Nombre y apellidos", example = "Ana Pérez")
    val fullName: String,
    @field:Email @field:NotBlank @field:Schema(description = "Correo único del usuario", example = "ana@example.com")
    val email: String,
    @field:NotBlank @field:Schema(description = "Teléfono (solo dígitos, único)", example = "612345678")
    val phone: String,
    @field:Size(min = 6) @field:Schema(description = "Contraseña, mínimo 6 caracteres", example = "123456")
    val password: String,
    @field:NotBlank @field:Schema(description = "Debe coincidir con password", example = "123456")
    val confirmPassword: String,
    @field:AssertTrue(message = "Debes aceptar los términos") @field:Schema(description = "Debe ser true para registrar", example = "true")
    val acceptedTerms: Boolean,
    @field:Schema(description = "Hijos a vincular; la app envía una lista vacía", example = "[]")
    val children: List<ChildInfoDto> = emptyList(),
    @field:Schema(description = "Grupos a crear; la app envía una lista vacía", example = "[]")
    val groups: List<GroupInputDto> = emptyList(),
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/auth/login`.
 *
 * Validación: `email` con formato de correo y no vacío; `password` no vacío. Las credenciales
 * se comprueban contra el hash almacenado; la respuesta es [TokenResponse].
 */
@Schema(description = "Credenciales de acceso; devuelve accessToken y refreshToken.")
data class LoginRequest(
    @field:Email @field:NotBlank @field:Schema(description = "Correo del usuario", example = "ana@example.com")
    val email: String,
    @field:NotBlank @field:Schema(description = "Contraseña del usuario", example = "123456")
    val password: String,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/auth/refresh` y `POST /api/v1/auth/logout`.
 *
 * Validación: `refreshToken` no vacío. En refresh se canjea por un nuevo par de tokens; en
 * logout se revoca el token almacenado (su hash) para la sesión del usuario.
 */
@Schema(description = "Token de refresco obtenido en login o registro.")
data class RefreshRequest(@field:NotBlank @field:Schema(description = "Token refresh en claro", example = "eyJhbGciOi...") val refreshToken: String)

/**
 * Capa: dto (respuesta). Resultado de `POST /auth/register`, `POST /auth/login` y
 * `POST /auth/refresh`.
 *
 * `user` es nullable porque en refresh la app ya tiene el perfil; `createdGroups` contiene los
 * grupos generados durante el registro (vacío en login/refresh); `expiresIn` es el tiempo de
 * validez del `accessToken` **en segundos**, no una fecha.
 */
data class TokenResponse(
    val user: UserDto? = null,
    val createdGroups: List<SchoolGroupDto> = emptyList(),
    @field:Schema(description = "JWT de acceso para el header Authorization: Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "Token para renovar la sesión; se envía en /auth/refresh", example = "eyJhbGciOi...")
    val refreshToken: String,
    @field:Schema(description = "Segundos de validez del accessToken", example = "3600")
    val expiresIn: Long,
)

/**
 * Capa: dto (respuesta). Cuerpo de `GET /api/v1/auth/exists/email` y
 * `GET /api/v1/auth/exists/phone`; la app lo usa para validar el formulario de registro.
 * Ejemplo: `{"exists":true}`.
 */
data class ExistsResponse(val exists: Boolean)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/auth/reset-code`.
 *
 * Validación: `email` con formato y no vacío. Si el usuario existe se genera un código de
 * 6 dígitos caducable; la respuesta es [ResetCodeResponse].
 */
@Schema(description = "Solicitud de código de restablecimiento de contraseña.")
data class ResetCodeRequest(@field:Email @field:NotBlank @field:Schema(description = "Correo de la cuenta", example = "ana@example.com") val email: String)

/**
 * Capa: dto (respuesta). Cuerpo de `POST /api/v1/auth/reset-code`.
 *
 * `resetCode` es nullable: el backend no filtra si el correo existe, para no revelar cuentas.
 * Ejemplo: `{"resetCode":"482915"}`.
 */
data class ResetCodeResponse(@field:Schema(description = "Código de 6 dígitos, null si el correo no existe", example = "482915") val resetCode: String? = null)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/auth/reset-password`.
 *
 * Validación: `email` con formato, `code` no vacío (6 dígitos en BD), `newPassword` de al
 * menos 6 caracteres y `confirmPassword` no vacío. Devuelve 200 vacío si el código es válido.
 */
@Schema(description = "Cambio de contraseña usando el código recibido por correo.")
data class ResetPasswordRequest(
    @field:Email @field:NotBlank @field:Schema(description = "Correo de la cuenta", example = "ana@example.com")
    val email: String,
    @field:NotBlank @field:Schema(description = "Código de 6 dígitos recibido", example = "482915")
    val code: String,
    @field:Size(min = 6) @field:Schema(description = "Nueva contraseña, mínimo 6 caracteres", example = "654321")
    val newPassword: String,
    @field:NotBlank @field:Schema(description = "Debe coincidir con newPassword", example = "654321")
    val confirmPassword: String,
)

/**
 * Capa: dto (respuesta). Grupo/clase devuelto por `GET /groups`, `GET /groups/by-code`,
 * `GET /groups/{id}` y `POST /groups`, y anidado en [TokenResponse] al registrar.
 *
 * `studentCount` es el contador cacheado de la entidad (se incrementa al vincular alumnos),
 * no un `count(*)` en tiempo real. Ejemplo: `{"id":3,"name":"3º A","school":"San Juan",
 * "subject":"Matemáticas","code":"AB-1234","teacherId":7,"studentCount":12}`.
 */
data class SchoolGroupDto(
    val id: Long,
    val name: String,
    val school: String,
    val subject: String,
    @field:Schema(description = "Código de invitación en mayúsculas, p. ej. AB-1234", example = "AB-1234")
    val code: String,
    val teacherId: Long,
    @field:Schema(description = "Número de alumnos cacheado en school_groups", example = "12")
    val studentCount: Int,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/groups`.
 *
 * Validación: `teacherId` obligatorio (`@NotNull`) y `groupName`, `schoolName`, `subject` no
 * vacíos. El backend genera el `code` único de invitación. Respuesta: [SchoolGroupDto] con 201.
 */
@Schema(description = "Creación de un grupo/clase por parte del docente.")
data class CreateGroupRequest(
    @field:NotNull @field:Schema(description = "Id del docente propietario del grupo", example = "7")
    val teacherId: Long,
    @field:NotBlank @field:Schema(description = "Nombre del grupo", example = "3º A")
    val groupName: String,
    @field:NotBlank @field:Schema(description = "Nombre del colegio", example = "Colegio San Juan")
    val schoolName: String,
    @field:NotBlank @field:Schema(description = "Materia del grupo", example = "Matemáticas")
    val subject: String,
)

/**
 * Capa: dto (respuesta). Alumno devuelto por `GET /groups/{id}/students`, `GET /students`
 * y `POST /students:link`.
 *
 * `parentId` usa el contrato de la app: `0` significa "sin tutor"; en la BD el valor es `NULL`
 * y el mapper hace la traducción. Ejemplo: `{"id":11,"name":"Lucía Pérez","groupId":3,
 * "parentId":7}`.
 */
data class StudentDto(
    val id: Long,
    val name: String,
    val groupId: Long,
    @field:Schema(description = "Id del padre/tutor; 0 equivale a 'sin tutor' (NULL en BD)", example = "7")
    val parentId: Long,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/students:link`.
 *
 * Validación: `parentId` obligatorio (`@NotNull`), `classCode` y `studentName` no vacíos. El
 * código de clase se busca ignorando mayúsculas; si no existe, la respuesta es 404. Al crear
 * el alumno se incrementa el `studentCount` del grupo (201 + [StudentDto]).
 */
@Schema(description = "Vinculación de un hijo con un grupo mediante el código de invitación.")
data class LinkStudentRequest(
    @field:NotNull @field:Schema(description = "Id del padre/tutor que vincula al alumno", example = "7")
    val parentId: Long,
    @field:NotBlank @field:Schema(description = "Código de invitación del grupo, p. ej. AB-1234", example = "AB-1234")
    val classCode: String,
    @field:NotBlank @field:Schema(description = "Nombre del alumno", example = "Lucía Pérez")
    val studentName: String,
)

/**
 * Capa: dto (respuesta). Tarea devuelta por `GET /tasks`, `GET /tasks/{id}`, `POST /tasks`,
 * `PUT /tasks/{id}` y `POST /tasks/{id}/deliveries`.
 *
 * `dueDate` y `publishedDate` se serializan como `YYYY-MM-DD`; `subject` viaja como el nombre
 * del enum; `hasPhoto` es la bandera de la app (el backend no guarda imágenes);
 * `deliveredStudentIds` procede de `task_deliveries` y `status` es **derivado** por
 * `TaskService` (`DELIVERED`, `EXPIRED` o `PENDING`; `null` en los listados).
 */
data class SchoolTaskDto(
    val id: Long,
    val groupId: Long,
    val title: String,
    @field:Schema(description = "Materia: MATEMATICAS, ESPANOL, CIENCIAS, HISTORIA u OTRA", example = "MATEMATICAS")
    val subject: Subject,
    val description: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd") @field:Schema(description = "Fecha límite YYYY-MM-DD", example = "2026-10-15")
    val dueDate: LocalDate,
    @field:JsonFormat(pattern = "yyyy-MM-dd") @field:Schema(description = "Fecha de publicación YYYY-MM-DD", example = "2026-10-01")
    val publishedDate: LocalDate,
    val teacherName: String,
    val totalStudents: Int,
    @field:Schema(description = "Indica si la tarea tiene foto en la app; el backend no gestiona el archivo", example = "false")
    val hasPhoto: Boolean,
    @field:Schema(description = "Ids de alumnos que ya entregaron; proceden de task_deliveries", example = "[11,12]")
    val deliveredStudentIds: List<Long>,
    @field:Schema(description = "Estado derivado: DELIVERED, EXPIRED o PENDING; null en listados", example = "PENDING")
    val status: String? = null,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/tasks`.
 *
 * Validación: `groupId` obligatorio; título, materia, descripción y `dateText` no vacíos.
 * `dateText` debe ser `YYYY-MM-DD` (ISO); un formato incorrecto responde 400 con
 * `FieldErrorDto("dateText", ...)`. `subject` usa el enum [Subject].
 */
@Schema(description = "Creación de una tarea; dateText usa el formato YYYY-MM-DD.")
data class CreateTaskRequest(
    @field:NotNull @field:Schema(description = "Id del grupo donde se publica", example = "3")
    val groupId: Long,
    @field:NotBlank @field:Schema(description = "Título de la tarea", example = "Ejercicios de fracciones")
    val title: String,
    @field:NotNull @field:Schema(description = "MATEMATICAS, ESPANOL, CIENCIAS, HISTORIA u OTRA", example = "MATEMATICAS")
    val subject: Subject,
    @field:NotBlank @field:Schema(description = "Enunciado de la tarea", example = "Resolver los ejercicios 1 a 10")
    val description: String,
    @field:NotBlank @field:Schema(description = "Fecha límite en formato YYYY-MM-DD", example = "2026-10-15")
    val dateText: String,
)

/**
 * Capa: dto (request). Cuerpo de `PUT /api/v1/tasks/{id}`.
 *
 * Validación: título, materia, descripción y `dateText` no vacíos; `dateText` en `YYYY-MM-DD`.
 * El `id` y el `groupId` se toman de la URL/registro existente, no del cuerpo.
 */
@Schema(description = "Edición de una tarea existente; dateText usa YYYY-MM-DD.")
data class UpdateTaskRequest(
    @field:NotBlank @field:Schema(description = "Título de la tarea", example = "Ejercicios de fracciones")
    val title: String,
    @field:NotNull @field:Schema(description = "MATEMATICAS, ESPANOL, CIENCIAS, HISTORIA u OTRA", example = "MATEMATICAS")
    val subject: Subject,
    @field:NotBlank @field:Schema(description = "Enunciado de la tarea", example = "Resolver los ejercicios 1 a 10")
    val description: String,
    @field:NotBlank @field:Schema(description = "Fecha límite en formato YYYY-MM-DD", example = "2026-10-15")
    val dateText: String,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/tasks/{id}/deliveries`.
 *
 * Validación: `studentId` obligatorio (`@NotNull`). El alumno debe pertenecer al grupo de la
 * tarea (si no, 409); la PK compuesta de `task_deliveries` hace idempotente marcar la entrega.
 */
@Schema(description = "Marcar como entregada una tarea para un alumno.")
data class MarkDeliveredRequest(@field:NotNull @field:Schema(description = "Id del alumno que entrega", example = "11") val studentId: Long)

/**
 * Capa: dto (respuesta). Junta devuelta por `GET /meetings`, `GET /meetings/{id}`,
 * `POST /meetings`, `PUT /meetings/{id}` y `PUT /meetings/{id}/responses/{studentId}`.
 *
 * `date` se serializa como `YYYY-MM-DD` y `time` es texto (p. ej. `"8:00 am"`). Campos
 * **derivados**, no persistidos tal cual en la respuesta: `hasVirtualLink` (`virtualLink != null`),
 * `isNext` (fecha >= hoy), `relativeLabel` (`"Hoy"`, `"Mañana"`, `"En N días"` o `null`) y
 * `response` (`PENDING` salvo que algún alumno haya respondido `ATTENDING`).
 */
data class MeetingDto(
    val id: Long,
    val groupId: Long,
    val title: String,
    @field:JsonFormat(pattern = "yyyy-MM-dd") @field:Schema(description = "Fecha de la junta YYYY-MM-DD", example = "2026-10-15")
    val date: LocalDate,
    @field:Schema(description = "Hora en texto, p. ej. 8:00 am", example = "8:00 am")
    val time: String,
    val location: String,
    @field:Schema(description = "Derivado de virtualLink: true si hay enlace", example = "true")
    val hasVirtualLink: Boolean,
    @field:Schema(description = "Derivado: true si la junta es hoy o futura", example = "true")
    val isNext: Boolean,
    @field:Schema(description = "Etiqueta derivada: Hoy, Mañana, En N días o null si ya pasó", example = "Mañana")
    val relativeLabel: String?,
    @field:Schema(description = "Respuesta derivada de las respuestas de las familias", example = "PENDING")
    val response: MeetingResponse,
    @field:Schema(description = "Enlace http(s) de la junta virtual, o null", example = "https://meet.example.com/aula")
    val virtualLink: String?,
    val reminderEnabled: Boolean,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/meetings`.
 *
 * Validación: `title`, `dateText`, `time` y `place` no vacíos (`place` se almacena como
 * `location`); `link` es opcional pero, si se envía, debe empezar por `http://` o `https://`;
 * `reminder` por defecto `true`; `groupId` obligatorio. `dateText` usa `YYYY-MM-DD` y una
 * junta duplicada en grupo/fecha/hora/lugar responde 409.
 */
@Schema(description = "Creación de una junta; dateText usa YYYY-MM-DD y place se guarda como location.")
data class CreateMeetingRequest(
    @field:NotBlank @field:Schema(description = "Título de la junta", example = "Reunión de padres")
    val title: String,
    @field:NotBlank @field:Schema(description = "Fecha en formato YYYY-MM-DD", example = "2026-10-15")
    val dateText: String,
    @field:NotBlank @field:Schema(description = "Hora en texto, p. ej. 8:00 am", example = "8:00 am")
    val time: String,
    @field:NotBlank @field:Schema(description = "Lugar de la junta (se persiste como location)", example = "Salón 3B")
    val place: String,
    @field:Schema(description = "Enlace opcional http(s) para la junta virtual", example = "https://meet.example.com/aula")
    val link: String? = null,
    @field:Schema(description = "Activar recordatorio; por defecto true", example = "true")
    val reminder: Boolean = true,
    @field:NotNull @field:Schema(description = "Id del grupo de la junta", example = "3")
    val groupId: Long,
)

/**
 * Capa: dto (request). Cuerpo de `PUT /api/v1/meetings/{id}`.
 *
 * Validación: misma que [CreateMeetingRequest] excepto `groupId`, que se mantiene del registro
 * existente. `dateText` en `YYYY-MM-DD`; el conflicto de horario ignora la propia junta.
 */
@Schema(description = "Edición de una junta existente; dateText usa YYYY-MM-DD.")
data class UpdateMeetingRequest(
    @field:NotBlank @field:Schema(description = "Título de la junta", example = "Reunión de padres")
    val title: String,
    @field:NotBlank @field:Schema(description = "Fecha en formato YYYY-MM-DD", example = "2026-10-15")
    val dateText: String,
    @field:NotBlank @field:Schema(description = "Hora en texto, p. ej. 8:00 am", example = "8:00 am")
    val time: String,
    @field:NotBlank @field:Schema(description = "Lugar de la junta (se persiste como location)", example = "Salón 3B")
    val place: String,
    @field:Schema(description = "Enlace opcional http(s); null si no hay", example = "https://meet.example.com/aula")
    val link: String? = null,
    @field:Schema(description = "Activar recordatorio", example = "true")
    val reminder: Boolean = true,
)

/**
 * Capa: dto (respuesta). Elemento de `PageResponse` devuelto por
 * `GET /api/v1/meetings/{id}/attendance` (vista del docente).
 *
 * Contiene el nombre del alumno, su respuesta y su id para que la app pueda navegar al alumno.
 * Ejemplo: `{"familyName":"Lucía Pérez","response":"ATTENDING","studentId":11}`.
 */
data class FamilyResponseDto(
    val familyName: String,
    @field:Schema(description = "PENDING, ATTENDING o DECLINED", example = "ATTENDING")
    val response: MeetingResponse,
    val studentId: Long,
)

/**
 * Capa: dto (request). Cuerpo de `PUT /api/v1/meetings/{id}/responses/{studentId}`.
 *
 * Validación: `response` obligatorio (`@NotNull`) y debe ser `PENDING`, `ATTENDING` o
 * `DECLINED`. La clave compuesta de `meeting_responses` permite reemplazar la respuesta
 * anterior sin duplicarla.
 */
@Schema(description = "Respuesta de una familia a una junta.")
data class SetResponseRequest(@field:NotNull @field:Schema(description = "PENDING, ATTENDING o DECLINED", example = "ATTENDING") val response: MeetingResponse)

/**
 * Capa: dto (respuesta). Cuerpo de `GET /api/v1/meetings/responses`.
 *
 * Mapa con el id de la junta como **clave en texto** (los ids de las claves JSON siempre son
 * cadenas) y el nombre del enum `MeetingResponse` como valor. Ejemplo:
 * `{"responses":{"12":"ATTENDING","13":"PENDING"}}`.
 */
data class ResponsesDto(@field:Schema(description = "Clave: id de junta como texto; valor: PENDING/ATTENDING/DECLINED") val responses: Map<String, String>)

/**
 * Capa: dto (respuesta). Cuerpo de `POST /api/v1/meetings/{id}/reminders`.
 *
 * Cuenta las respuestas `PENDING` de la junta para saber a cuántas familias hay que avisar.
 * Ejemplo: `{"pending":3}`.
 */
data class PendingDto(val pending: Int)

/**
 * Capa: dto (respuesta). Resumen **derivado** de asistencia, anidado en [CalendarEventDto]
 * para las juntas de la vista del docente.
 *
 * Los tres contadores se obtienen agrupando `meeting_responses` por respuesta; no se almacenan.
 * Ejemplo: `{"confirmed":4,"declined":1,"pending":3}`.
 */
data class AttendanceSummaryDto(
    val confirmed: Int,
    val declined: Int,
    val pending: Int,
)

/**
 * Capa: dto (respuesta). Evento devuelto por `GET /api/v1/calendar/events` y
 * `POST /api/v1/calendar/events`.
 *
 * `date` se serializa como `YYYY-MM-DD`; `type` viaja como el nombre del enum. El `id` de un
 * evento de junta es `1000 + meetings.id` (`MEETING_ID_OFFSET`) para no colisionar con los id
 * de tareas en la app original; el de una tarea es el id de la tarea. `attendance` es derivado
 * y solo se rellena para juntas en la vista del docente (`role=TEACHER`), nunca para padres.
 */
data class CalendarEventDto(
    @field:Schema(description = "Id del evento; las juntas usan 1000 + meetings.id", example = "1012")
    val id: Long,
    val groupId: Long,
    @field:JsonFormat(pattern = "yyyy-MM-dd") @field:Schema(description = "Fecha del evento YYYY-MM-DD", example = "2026-10-15")
    val date: LocalDate,
    @field:Schema(description = "TASK, MEETING o EXAM", example = "MEETING")
    val type: CalendarEventType,
    val title: String,
    val subtitle: String,
    @field:Schema(description = "Asistencia derivada; solo para juntas y solo para el docente")
    val attendance: AttendanceSummaryDto? = null,
)

/**
 * Capa: dto (request). Cuerpo de `POST /api/v1/calendar/events` (exámenes u otros eventos).
 *
 * Validación: `groupId`, `date` y `type` obligatorios (`@NotNull`), `title` no vacío; `subtitle`
 * es opcional (`""` por defecto). `date` se deserializa como `YYYY-MM-DD`. El backend asigna
 * el id con `calendar_events_id_seq`; las juntas/tareas crean su evento de forma automática.
 */
@Schema(description = "Creación de un evento de calendario; date usa YYYY-MM-DD.")
data class CreateCalendarEventRequest(
    @field:NotNull @field:Schema(description = "Id del grupo del evento", example = "3")
    val groupId: Long,
    @field:NotNull @field:Schema(description = "Fecha del evento en formato YYYY-MM-DD", example = "2026-10-15")
    val date: LocalDate,
    @field:NotNull @field:Schema(description = "TASK, MEETING o EXAM", example = "EXAM")
    val type: CalendarEventType,
    @field:NotBlank @field:Schema(description = "Título del evento", example = "Examen final")
    val title: String,
    @field:Schema(description = "Subtítulo opcional", example = "Matemáticas")
    val subtitle: String = "",
)

/**
 * Capa: dto (respuesta). Detalle de error de validación dentro de la propiedad `errors` de un
 * `ProblemDetail` (RFC 9457), emitido por `GlobalExceptionHandler` con estado 400 u otros
 * errores de dominio controlados.
 *
 * `field` es el nombre del campo de la request (o `"body"`/`objectName` cuando no hay campo),
 * y `message` el texto legible en español que la app muestra al usuario. Ejemplo dentro de la
 * respuesta: `{"type":"about:blank","title":"Bad Request","status":400,
 * "detail":"Validación incorrecta","errors":[{"field":"email","message":"must be a well-formed email address"}]}`.
 */
data class FieldErrorDto(
    @field:Schema(description = "Campo de la request que falló, o body/objectName", example = "email")
    val field: String,
    @field:Schema(description = "Mensaje de error para el usuario", example = "Debes aceptar los términos")
    val message: String,
)
