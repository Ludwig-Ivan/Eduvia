package com.example.librerias.api.repository

import com.example.librerias.api.domain.CalendarEvent
import com.example.librerias.api.domain.Meeting
import com.example.librerias.api.domain.MeetingResponse
import com.example.librerias.api.domain.MeetingResponseEntity
import com.example.librerias.api.domain.MeetingResponseId
import com.example.librerias.api.domain.TaskDeliveryId
import com.example.librerias.api.domain.PasswordResetCode
import com.example.librerias.api.domain.RefreshToken
import com.example.librerias.api.domain.SchoolGroup
import com.example.librerias.api.domain.SchoolTask
import com.example.librerias.api.domain.Student
import com.example.librerias.api.domain.TaskDelivery
import com.example.librerias.api.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/**
 * Capa: repository (Spring Data JPA) para la entidad `User` / tabla `users`.
 *
 * Responsabilidad: buscar usuarios por email o teléfono ignorando mayúsculas y comprobar su
 * existencia antes de registrarlos. No define `@Query`: todas son consultas derivadas del
 * nombre del método.
 *
 * Colaboradores: `AuthService` y `TaskService` (para resolver el nombre del docente al crear
 * una tarea).
 *
 * Endpoints atendidos: `POST /auth/register` (unicidad de email/teléfono), `POST /auth/login`
 * (email del usuario, sin distinción de mayúsculas), `GET /auth/exists/email`,
 * `GET /auth/exists/phone` y `GET /me`.
 */
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailIgnoreCase(email: String): User?
    fun findByPhone(phone: String): User?
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun existsByPhone(phone: String): Boolean
}

/**
 * Capa: repository (Spring Data JPA) para `RefreshToken` / tabla `refresh_tokens`.
 *
 * Responsabilidad: localizar un token de refresco por su hash y listar los tokens vigentes
 * de un usuario (no revocados y no caducados). Solo consultas derivadas, sin `@Query`.
 *
 * Colaboradores: `AuthService`, que valida el hash del token presentado en refresh y logout.
 *
 * Endpoints atendidos: `POST /auth/refresh` y `POST /auth/logout` (ambos con
 * `findByTokenHash`). `findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter` está disponible para
 * revocar todas las sesiones vigentes de un usuario; el logout actual revoca únicamente el
 * token enviado por la app.
 */
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId: Long, now: Instant): List<RefreshToken>
}

/**
 * Capa: repository (Spring Data JPA) para `PasswordResetCode` / tabla `password_reset_codes`.
 *
 * Responsabilidad: recuperar el código de restablecimiento más reciente aún no usado
 * (`ORDER BY id DESC`) y listar todos los códigos del usuario. Solo consultas derivadas.
 *
 * Colaboradores: `AuthService`, que valida caducidad e intentos restantes sobre el código.
 *
 * Endpoints atendidos: `POST /auth/reset-password` (`findTopByUserIdAndUsedAtIsNullOrderByIdDesc`)
 * y `POST /auth/reset-code` (historial del usuario para limpiar códigos anteriores).
 */
interface PasswordResetCodeRepository : JpaRepository<PasswordResetCode, Long> {
    fun findTopByUserIdAndUsedAtIsNullOrderByIdDesc(userId: Long): PasswordResetCode?
    fun findAllByUserId(userId: Long): List<PasswordResetCode>
}

/**
 * Capa: repository (Spring Data JPA) para `SchoolGroup` / tabla `school_groups`.
 *
 * Responsabilidad: listar los grupos de un docente **con paginación estable** (el controlador
 * añade `Sort.by("id")` para que las páginas no se repitan) y resolver el código de invitación
 * de forma case-insensitive; `existsByCodeIgnoreCase` evita códigos duplicados al generarlos.
 * No hay `@Query`: son consultas derivadas.
 *
 * Colaboradores: `GroupService`, `TaskService`, `MeetingService` y `CalendarService` (validación
 * de existencia del grupo).
 *
 * Endpoints atendidos: `GET /groups?teacherId=` (`findAllByTeacherId`), `GET /groups/by-code`
 * y `POST /students:link` (`findByCodeIgnoreCase`), y creación de grupos (`existsByCodeIgnoreCase`).
 */
interface SchoolGroupRepository : JpaRepository<SchoolGroup, Long> {
    fun findAllByTeacherId(teacherId: Long, pageable: Pageable): Page<SchoolGroup>
    fun findByCodeIgnoreCase(code: String): SchoolGroup?
    fun existsByCodeIgnoreCase(code: String): Boolean
}

/**
 * Capa: repository (Spring Data JPA) para `Student` / tabla `students`.
 *
 * Responsabilidad: listar alumnos por grupo o por padre con paginación estable (el controlador
 * ordena por `id`) y devolver todos los alumnos de un grupo sin paginar. Solo consultas
 * derivadas, sin `@Query`.
 *
 * Colaboradores: `GroupService` (listados y vinculación) y `TaskService` (comprobar que el
 * alumno pertenece al grupo de la tarea antes de marcar la entrega).
 *
 * Endpoints atendidos: `GET /groups/{id}/students` (`findAllByGroupId(groupId, pageable)`),
 * `GET /students?parentId=` (`findAllByParentId`) y `POST /tasks/{id}/deliveries`
 * (`findAllByGroupId(groupId)` en la validación de pertenencia).
 */
interface StudentRepository : JpaRepository<Student, Long> {
    fun findAllByGroupId(groupId: Long, pageable: Pageable): Page<Student>
    fun findAllByParentId(parentId: Long, pageable: Pageable): Page<Student>
    fun findAllByGroupId(groupId: Long): List<Student>
}

/**
 * Capa: repository (Spring Data JPA) para `SchoolTask` / tabla `school_tasks`.
 *
 * Responsabilidad: listar tareas de un grupo con paginación estable (ordenadas por `dueDate, id`
 * desde el controlador) y ejecutar la **consulta personalizada** `findDeliveredStudentIds`, que
 * proyecta solo los ids de alumnos que ya entregaron la tarea (evita cargar entidades).
 *
 * Colaboradores: `TaskService`, que usa esa lista para `SchoolTaskDto.deliveredStudentIds` y
 * para derivar el `status` (`DELIVERED`/`EXPIRED`/`PENDING`).
 *
 * Endpoints atendidos: `GET /tasks?groupId=` (listado paginado), `GET /tasks/{id}`,
 * `POST /tasks`, `PUT /tasks/{id}` y `POST /tasks/{id}/deliveries`.
 */
interface SchoolTaskRepository : JpaRepository<SchoolTask, Long> {
    fun findAllByGroupId(groupId: Long, pageable: Pageable): Page<SchoolTask>

    /** Ids de alumnos que entregaron la tarea; alimenta `deliveredStudentIds` y el `status` derivado. */
    @Query("select d.student.id from TaskDelivery d where d.task.id = :taskId")
    fun findDeliveredStudentIds(@Param("taskId") taskId: Long): List<Long>
}

/**
 * Capa: repository (Spring Data JPA) para `TaskDelivery` / tabla `task_deliveries`
 * (clave compuesta [TaskDeliveryId]).
 *
 * Responsabilidad: comprobar si un alumno ya entregó una tarea (idempotencia) y borrar en bloque
 * todas las entregas de una tarea mediante la **consulta de modificación** `deleteByTaskId`.
 *
 * Colaboradores: `TaskService`; la PK compuesta es la que impide duplicados en la base de datos.
 *
 * Endpoints atendidos: `POST /tasks/{id}/deliveries` (`existsByTaskIdAndStudentId` antes de
 * guardar) y `DELETE /tasks/{id}` (`deleteByTaskId` en la misma transacción).
 */
interface TaskDeliveryRepository : JpaRepository<TaskDelivery, TaskDeliveryId> {
    fun existsByTaskIdAndStudentId(taskId: Long, studentId: Long): Boolean

    /** Borrado masivo previo a eliminar la tarea; evita borrar las entregas una a una. */
    @Modifying
    @Query("delete from TaskDelivery d where d.task.id = :taskId")
    fun deleteByTaskId(@Param("taskId") taskId: Long)
}

/**
 * Capa: repository (Spring Data JPA) para `Meeting` / tabla `meetings`.
 *
 * Responsabilidad: listar juntas con **paginación estable** (`findAllBy(pageable)`, con
 * `Sort.by("date", "id")` en el controlador) y ejecutar la **consulta personalizada**
 * `findConflictingTimes`, que devuelve las horas en minúsculas de otras juntas del mismo grupo,
 * fecha y lugar. El parámetro `excludeId` permite ignorar la propia junta al editar (en el alta
 * se pasa `-1`).
 *
 * Colaboradores: `MeetingService`, que convierte cualquier coincidencia en un **409 Conflict**
 * con `GlobalExceptionHandler`, y `CalendarService` (existencia de la junta para la asistencia).
 *
 * Endpoints atendidos: `GET /meetings` (`findAllBy`), `POST /meetings` y `PUT /meetings/{id}`
 * (`findConflictingTimes`), `DELETE /meetings/{id}` y `GET /meetings/{id}/attendance`.
 */
interface MeetingRepository : JpaRepository<Meeting, Long> {
    fun findAllBy(pageable: Pageable): Page<Meeting>

    /**
     * Horas (en minúsculas) de juntas que chocan en grupo/fecha/lugar; base del 409 de juntas.
     * Compara con `lower(...)` para coincidir con el índice funcional único de la tabla.
     */
    @Query(
        "select lower(m.time) from Meeting m where m.groupId = :groupId and m.date = :date " +
            "and lower(m.location) = lower(:location) and m.id <> :excludeId"
    )
    fun findConflictingTimes(
        @Param("groupId") groupId: Long,
        @Param("date") date: java.time.LocalDate,
        @Param("location") location: String,
        @Param("excludeId") excludeId: Long,
    ): List<String>
}

/**
 * Capa: repository (Spring Data JPA) para `MeetingResponseEntity` / tabla `meeting_responses`
 * (clave compuesta [MeetingResponseId]).
 *
 * Responsabilidad: buscar la respuesta de un alumno a una junta (para reemplazarla en lugar de
 * duplicarla), listar respuestas por junta o por alumno, contar las de cada estado y borrar en
 * bloque las respuestas de una junta con la **consulta de modificación** `deleteByMeetingId`.
 *
 * Colaboradores: `MeetingService` y `CalendarService`; la PK compuesta garantiza una respuesta
 * por par (junta, alumno).
 *
 * Endpoints atendidos: `GET /meetings` y `GET /meetings/{id}` (`countByMeetingIdAndResponse` para
 * derivar `MeetingDto.response`), `GET /meetings/responses` (`findAllByStudentId`),
 * `PUT /meetings/{id}/responses/{studentId}` (`findByMeetingIdAndStudentId`),
 * `POST /meetings/{id}/reminders` (conteo de `PENDING`), `GET /meetings/{id}/attendance`
 * (`findAllByMeetingId`) y `DELETE /meetings/{id}` (`deleteByMeetingId`).
 */
interface MeetingResponseRepository : JpaRepository<MeetingResponseEntity, MeetingResponseId> {
    fun findByMeetingIdAndStudentId(meetingId: Long, studentId: Long): MeetingResponseEntity?
    fun findAllByMeetingId(meetingId: Long): List<MeetingResponseEntity>
    fun findAllByStudentId(studentId: Long): List<MeetingResponseEntity>
    fun countByMeetingIdAndResponse(meetingId: Long, response: MeetingResponse): Int

    /** Borrado masivo previo a eliminar la junta; evita borrar respuestas una a una. */
    @Modifying
    @Query("delete from MeetingResponseEntity r where r.meeting.id = :meetingId")
    fun deleteByMeetingId(@Param("meetingId") meetingId: Long)
}

/**
 * Capa: repository (Spring Data JPA) para `CalendarEvent` / tabla `calendar_events`.
 *
 * Responsabilidad: recuperar eventos de un grupo y tipo ordenados por fecha. Es la única
 * consulta derivada declarada aquí (sin `@Query`): el id de los eventos no se genera en este
 * repositorio, sino que lo suministran tarea/junta o la secuencia de la BD.
 *
 * Colaboradores: `CalendarService` y `MeetingService` (sincronización del evento `1000 + id`
 * de cada junta). El listado paginado actual de `GET /calendar/events` usa `findAll()` y ordena
 * en memoria; este método cubre las consultas filtradas por grupo y tipo del calendario.
 *
 * Endpoints atendidos: `GET /calendar/events` (filtrado por grupo/tipo), `POST /calendar/events`
 * y `DELETE /calendar/events/{id}`.
 */
interface CalendarEventRepository : JpaRepository<CalendarEvent, Long> {
    fun findAllByGroupIdAndTypeOrderByDateAsc(groupId: Long, type: com.example.librerias.api.domain.CalendarEventType): List<CalendarEvent>
}
