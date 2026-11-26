package com.example.librerias.api.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * Capa: entity (JPA). Representa la tabla `users`.
 *
 * Responsabilidad: persistir la identidad de docentes y padres/tutores usada por el JWT y por
 * los listados de la app. Colabora con `RefreshToken`, `PasswordResetCode`, `SchoolGroup`
 * (como `teacherId`) y `Student` (como `parentId`).
 *
 * Campos no obvios:
 * - `role`: `PARENT` o `TEACHER` (`UserRole`); determina el flujo de registro, el Dueño de los
 *   grupos y si `GET /api/v1/calendar/events` incluye asistencia. Se guarda como texto, nunca
 *   como ordinal, para que el contrato JSON de la app (`kotlinx.serialization`) coincida.
 * - `passwordHash`: hash de la contraseña; la contraseña en claro nunca se persiste.
 * - `createdAt`/`updatedAt`: auditoría (`timestamptz`), con valor inicial `Instant.now()`;
 *   `updatedAt` se refresca en las operaciones de escritura de los servicios.
 *
 * Existe para dar un único origen de autenticación y autorización a la aplicación móvil.
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "full_name", nullable = false, length = 200)
    var fullName: String = "",

    @Column(nullable = false, length = 320)
    var email: String = "",

    @Column(nullable = false, length = 20)
    var phone: String = "",

    /** Rol de la app: `PARENT` (padre/tutor) o `TEACHER` (docente); se persiste como texto. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var role: UserRole = UserRole.PARENT,

    /** Hash de la contraseña; nunca el valor introducido por el usuario. */
    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String = "",

    /** Auditoría: momento de creación del usuario. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última modificación del usuario. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (enum persistido). Valores posibles de `users.role`.
 *
 * Responsabilidad: modelar los dos perfiles de la app, `PARENT` y `TEACHER`, y viajar con el
 * mismo nombre en el JSON que consume `kotlinx.serialization`. Se almacena con
 * `@Enumerated(EnumType.STRING)` para no depender del orden de declaración.
 *
 * Colaboradores: `User`, `RegisterRequest`, `UserDto` y el filtro `role` de
 * `GET /api/v1/calendar/events`. Existe para evitar cadenas libres en la distinción
 * padre/docente, que cambia el comportamiento del calendario.
 */
enum class UserRole { PARENT, TEACHER }

/**
 * Capa: entity (JPA). Representa la tabla `refresh_tokens` (índice por `user_id`).
 *
 * Responsabilidad: guardar los tokens de refresco emitidos en login/registro para poder
 * revocarlos en logout y reemitir `accessToken` en `POST /auth/refresh`.
 *
 * Colaboradores: `User` (por `userId`), `AuthService` y `RefreshTokenRepository`.
 *
 * Campos no obvios: `tokenHash` es el hash del token (no el token en claro) y es único;
 * `revokedAt` es nullable —`null` significa token aún no revocado—; `expiresAt` marca su
 * caducidad; `createdAt` es la auditoría de emisión.
 *
 * Existe para soportar el ciclo access + refresh sin almacenar secretos en claro en la BD.
 */
@Entity
@Table(name = "refresh_tokens", indexes = [Index(name = "idx_refresh_tokens_user", columnList = "user_id")])
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    /** Hash del token de refresco; el valor en claro solo viaja en la respuesta del login. */
    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = "",

    /** Instante a partir del cual el token deja de ser válido. */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    /** `null` mientras no se haya hecho logout; si no es null, el token está revocado. */
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    /** Auditoría: momento en que se emitió el token. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `password_reset_codes` (índice por `user_id`).
 *
 * Responsabilidad: persistir el código de 6 dígitos enviado por `POST /auth/reset-code` y
 * verificarlo en `POST /auth/reset-password`.
 *
 * Colaboradores: `User` (por `userId`), `AuthService` y `PasswordResetCodeRepository`.
 *
 * Campos no obvios: `attemptsLeft` (empieza en 5) limita los intentos; `usedAt` es nullable y
 * solo se rellena al consumir el código; `expiresAt` limita su validez; `createdAt` es la
 * auditoría de generación.
 *
 * Existe para recuperar la contraseña con un código caducable de un solo uso sin guardar
 * contraseñas en claro.
 */
@Entity
@Table(name = "password_reset_codes", indexes = [Index(name = "idx_password_reset_codes_user", columnList = "user_id")])
class PasswordResetCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    /** Código numérico de 6 dígitos validado por la BD (`^[0-9]{6}$`). */
    @Column(nullable = false, length = 6)
    var code: String = "",

    /** Instante de caducidad del código. */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    /** Intentos restantes antes de descartar el código; la BD exige un valor >= 0. */
    @Column(name = "attempts_left", nullable = false)
    var attemptsLeft: Int = 5,

    /** `null` mientras el código no se haya usado; se rellena al restablecer la contraseña. */
    @Column(name = "used_at")
    var usedAt: Instant? = null,

    /** Auditoría: momento en que se generó el código. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `school_groups` (índice por `teacher_id`).
 *
 * Responsabilidad: persistir los grupos/clases de la app (nombre, colegio, materia y código
 * corto de invitación) y ser el contenedor de alumnos, tareas, juntas y eventos.
 *
 * Colaboradores: `User` (docente, por `teacherId`), `Student`, `SchoolTask`, `Meeting` y
 * `CalendarEvent`. Lo usan `GroupService`, `TaskService`, `MeetingService` y `CalendarService`.
 *
 * Campos no obvios:
 * - `code`: código único en mayúsculas generado por `GroupService`; la búsqueda es
 *   case-insensitive porque el usuario lo escribe desde la app (`GET /groups/by-code`,
 *   `POST /students:link`).
 * - `studentCount`: **contador cacheado**, no calculado con `count(*)`. Se incrementa en
 *   `linkStudent` cuando un padre vincula a un hijo y se usa como `totalStudents` inicial al
 *   crear una tarea.
 * - `createdAt`/`updatedAt`: auditoría de alta y última modificación.
 *
 * Existe para que el docente administre sus clases y para resolver el código de invitación
 * que la app introduce al vincular alumnos.
 */
@Entity
@Table(name = "school_groups", indexes = [Index(name = "idx_school_groups_teacher", columnList = "teacher_id")])
class SchoolGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(nullable = false, length = 200)
    var school: String = "",

    @Column(nullable = false, length = 200)
    var subject: String = "",

    /** Código de invitación único; se compara ignorando mayúsculas/minúsculas. */
    @Column(nullable = false, length = 16)
    var code: String = "",

    @Column(name = "teacher_id", nullable = false)
    var teacherId: Long = 0,

    /** Contador cacheado de alumnos: se incrementa en `linkStudent`, no se calcula por consulta. */
    @Column(name = "student_count", nullable = false)
    var studentCount: Int = 0,

    /** Auditoría: momento de creación del grupo. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última modificación del grupo. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `students` (índices por `group_id` y `parent_id`).
 *
 * Responsabilidad: persistir a los alumnos de cada grupo y su vínculo opcional con el padre
 * que los registra desde la app.
 *
 * Colaboradores: `SchoolGroup` (por `groupId`), `User` (por `parentId`), `TaskDelivery`,
 * `MeetingResponseEntity` y los servicios `GroupService`, `TaskService` y `MeetingService`.
 *
 * Campos no obvios:
 * - `parentId`: **nullable**. En BD, `NULL` = alumno sin tutor; en la app el valor
 *   ausente se representa con `0`, por lo que `Student.toDto()` traduce `NULL` a `0L` y
 *   `POST /students:link` recibe `parentId` como `Long`.
 * - `createdAt`/`updatedAt`: auditoría de alta y última modificación.
 *
 * Existe para poder listar hijos por padre (`GET /students`) y alumnos por grupo
 * (`GET /groups/{id}/students`), además de registrar entregas y respuestas a juntas.
 */
@Entity
@Table(name = "students", indexes = [Index(name = "idx_students_group", columnList = "group_id"), Index(name = "idx_students_parent", columnList = "parent_id")])
class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    /** `NULL` en BD = sin tutor; la app lo representa como `0` (ver `Student.toDto`). */
    @Column(name = "parent_id")
    var parentId: Long? = null,

    /** Auditoría: momento de creación del alumno. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última modificación del alumno. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `school_tasks`.
 *
 * Responsabilidad: persistir las tareas que el docente publica en un grupo: título, materia,
 * descripción, fecha de entrega y publicación, y el nombre del docente.
 *
 * Colaboradores: `SchoolGroup` (por `groupId`), `Subject`, `TaskDelivery` y `CalendarEvent`
 * (el evento de tipo `TASK` comparte el mismo `id` que la tarea).
 *
 * Campos no obvios:
 * - `subject`: enum `Subject` persistido como texto; la app usa los mismos nombres.
 * - `dueDate`/`publishedDate`: `LocalDate`, serializados en JSON como `YYYY-MM-DD`.
 * - `totalStudents`: instantánea de `SchoolGroup.studentCount` al crear la tarea; sirve como
 *   denominador para calcular el `status` derivado (`DELIVERED`/`EXPIRED`/`PENDING`) en
 *   `TaskService`, que **no se persiste** en esta tabla.
 * - `hasPhoto`: bandera heredada del modelo de la app que indica si la tarea incluye foto;
 *   el backend solo la persiste y la devuelve, no gestiona ningún archivo.
 * - `teacherName`: nombre copiado del `User` en el momento de la creación.
 * - `createdAt`/`updatedAt`: auditoría de creación y modificación (esta última se actualiza
 *   al editar la tarea).
 *
 * Existe para alimentar los listados de tareas vigentes/vencidas/entregadas de la app y para
 * derivar el estado que la app calculaba en memoria.
 */
@Entity
@Table(name = "school_tasks")
class SchoolTask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    @Column(nullable = false, length = 200)
    var title: String = "",

    /** Materia de la tarea, persistida como texto (enum `Subject`). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var subject: Subject = Subject.OTRA,

    @Column(nullable = false, columnDefinition = "text")
    var description: String = "",

    /** Fecha límite en formato `YYYY-MM-DD`; se usa para derivar `EXPIRED`. */
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate = LocalDate.now(),

    /** Fecha de publicación en formato `YYYY-MM-DD`. */
    @Column(name = "published_date", nullable = false)
    var publishedDate: LocalDate = LocalDate.now(),

    @Column(name = "teacher_name", nullable = false, length = 100)
    var teacherName: String = "",

    /** Instantánea de `SchoolGroup.studentCount` al crear la tarea; base del `status` derivado. */
    @Column(name = "total_students", nullable = false)
    var totalStudents: Int = 0,

    /** Indica si la tarea lleva foto en la app; el backend no almacena la imagen. */
    @Column(name = "has_photo", nullable = false)
    var hasPhoto: Boolean = false,

    /** Auditoría: momento de creación de la tarea. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última edición de la tarea. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (enum persistido). Materias posibles de una tarea en `school_tasks.subject`.
 *
 * Responsabilidad: exponer los mismos valores que el enum `Subject` de la app
 * (`MATEMATICAS`, `ESPANOL`, `CIENCIAS`, `HISTORIA`, `OTRA`) y persistirlos como texto.
 *
 * Colaboradores: `SchoolTask`, `CreateTaskRequest` y `UpdateTaskRequest`. Existe para que el
 * contrato JSON no dependa del orden de los valores ni acepte materias fuera del catálogo.
 */
enum class Subject { MATEMATICAS, ESPANOL, CIENCIAS, HISTORIA, OTRA }

/**
 * Capa: entity (JPA). Representa la tabla `task_deliveries`, con PK compuesta
 * `TaskDeliveryId(task, student)`.
 *
 * Responsabilidad: registrar qué alumnos han entregado cada tarea (`deliveredAt`).
 *
 * Colaboradores: `SchoolTask` y `Student` (ambos `@ManyToOne(LAZY)`), `TaskDeliveryRepository`
 * y `TaskService`. La tabla es la fuente de `SchoolTaskDto.deliveredStudentIds` y del
 * `status` derivado de la tarea.
 *
 * Campos no obvios: la **PK compuesta** existe por idempotencia —un alumno solo puede entregar
 * una tarea una vez, aunque pulse dos veces en `POST /tasks/{id}/deliveries`— y la relación
 * lazy evita cargar tarea y alumno al consultar solo identificadores.
 *
 * Existe para sustituir el conjunto `deliveredStudentIds` que la app mantenía en memoria.
 */
@Entity
@Table(name = "task_deliveries")
@IdClass(TaskDeliveryId::class)
class TaskDelivery(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    var task: SchoolTask,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    var student: Student,

    /** Momento en que se registró la entrega. */
    @Column(name = "delivered_at", nullable = false)
    var deliveredAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `meetings`.
 *
 * Responsabilidad: persistir las juntas escolares: grupo, título, fecha, hora como texto
 * (p. ej. `"8:00 am"`), lugar, enlace virtual opcional y recordatorio.
 *
 * Colaboradores: `SchoolGroup` (por `groupId`), `MeetingResponseEntity`, `MeetingRepository`
 * y `CalendarEvent` (el evento de una junta usa `MEETING_ID_OFFSET + id`).
 *
 * Campos no obvios:
 * - `time` y `location` se normalizan (trim) en el servicio y se comparan ignorando mayúsculas
 *   para detectar conflictos; la BD refuerza la unicidad con el índice funcional
 *   `(group_id, date, lower(time), lower(location))`.
 * - `virtualLink` es nullable; `hasVirtualLink` es un flag derivado que el servicio mantiene
 *   sincronizado con `virtualLink != null` (el mapper también lo deriva en la respuesta).
 * - `createdAt`/`updatedAt`: auditoría de creación y última edición de la junta.
 *
 * Existe para el listado de juntas de la app (`GET /meetings`, ordenado por fecha) y para
 * gestionar las respuestas de las familias y los avisos.
 */
@Entity
@Table(name = "meetings")
class Meeting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    @Column(nullable = false, length = 200)
    var title: String = "",

    /** Fecha de la junta en formato `YYYY-MM-DD`. */
    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),

    /** Hora en texto tal como la escribe la app (p. ej. `"8:00 am"`); se compara en minúsculas. */
    @Column(nullable = false, length = 20)
    var time: String = "",

    /** Lugar de la junta; la detección de conflictos lo compara ignorando mayúsculas. */
    @Column(nullable = false, length = 200)
    var location: String = "",

    /** Derivado de `virtualLink`: `true` cuando hay enlace para la reunión virtual. */
    @Column(name = "has_virtual_link", nullable = false)
    var hasVirtualLink: Boolean = false,

    /** Enlace http(s) opcional; `null` cuando la junta es presencial sin enlace. */
    @Column(name = "virtual_link", length = 500)
    var virtualLink: String? = null,

    @Column(name = "reminder_enabled", nullable = false)
    var reminderEnabled: Boolean = true,

    /** Auditoría: momento de creación de la junta. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última modificación de la junta. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (JPA). Representa la tabla `meeting_responses`, con PK compuesta
 * `MeetingResponseId(meeting, student)`.
 *
 * Responsabilidad: guardar la respuesta de cada alumno (`PENDING`, `ATTENDING`, `DECLINED`)
 * a una junta y cuándo se actualizó.
 *
 * Colaboradores: `Meeting` y `Student` (ambos `@ManyToOne(LAZY)`), el enum `MeetingResponse`,
 * `MeetingResponseRepository` y los DTOs `MeetingDto`, `FamilyResponseDto` y
 * `AttendanceSummaryDto`.
 *
 * Campos no obvios: la **PK compuesta** da idempotencia a
 * `PUT /meetings/{id}/responses/{studentId}` (una respuesta por familia y junta, reemplazable);
 * `response` se persiste como texto y `updatedAt` es el único campo de auditoría de esta tabla
 * (no tiene `createdAt`). A partir de estos datos se derivan `MeetingDto.response` y el
 * resumen de asistencia que ve el docente.
 *
 * Existe para reemplazar el mapa de respuestas en memoria de la app original.
 */
@Entity
@Table(name = "meeting_responses")
@IdClass(MeetingResponseId::class)
class MeetingResponseEntity(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    var meeting: Meeting,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    var student: Student,

    /** Estado de la respuesta, persistido como texto; `PENDING` es el valor inicial. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var response: MeetingResponse = MeetingResponse.PENDING,

    /** Auditoría: última vez que la familia cambió su respuesta. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/**
 * Capa: entity (enum persistido). Respuesta de una familia a una junta.
 *
 * Responsabilidad: modelar `PENDING` (sin confirmar), `ATTENDING` (asistirá) y `DECLINED`
 * (no asistirá), con el mismo nombre que usa la app en JSON. Se persiste como texto en
 * `meeting_responses.response`.
 *
 * Colaboradores: `MeetingResponseEntity`, `SetResponseRequest`, `FamilyResponseDto` y
 * `AttendanceSummaryDto`. Existe para derivar la asistencia agregada y el estado `response`
 * devuelto en `MeetingDto`.
 */
enum class MeetingResponse { PENDING, ATTENDING, DECLINED }

/**
 * Capa: entity (JPA). Representa la tabla `calendar_events`.
 *
 * Responsabilidad: persistir la vista unificada del calendario de la app (tareas, juntas y
 * exámenes) con fecha, título y subtítulo.
 *
 * Colaboradores: `SchoolGroup` (por `groupId`), `SchoolTask`, `Meeting`, `CalendarEventType`,
 * `CalendarEventRepository` y `CalendarService`. El `id` **no es autogenerado por JPA**: la
 * tarea usa su propio id y la junta usa `MEETING_ID_OFFSET + meetings.id`; los exámenes toman
 * el siguiente valor de la secuencia `calendar_events_id_seq`.
 *
 * Campos no obvios:
 * - `MEETING_ID_OFFSET = 1000` evita la **colisión de ids entre tareas y juntas** en la app
 *   original (ambas listas usaban ids que podían solaparse); `CalendarService` revierte el
 *   desplazamiento restando 1000 para consultar la asistencia de la junta.
 * - `type` se persiste como texto y determina si el evento puede tener `attendance` (solo
 *   juntas y solo en la vista del docente).
 * - `createdAt`/`updatedAt`: auditoría del evento.
 *
 * Existe para servir `GET /api/v1/calendar/events` sin repetir lógica de unión en el cliente.
 */
@Entity
@Table(name = "calendar_events")
class CalendarEvent(
    @Id
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,

    /** Fecha del evento en formato `YYYY-MM-DD`. */
    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),

    /** Tipo de evento, persistido como texto; condiciona el campo `attendance` derivado. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var type: CalendarEventType = CalendarEventType.EXAM,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(nullable = false, length = 200)
    var subtitle: String = "",

    /** Auditoría: momento de creación del evento. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Auditoría: última modificación del evento. */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * Según DECISIONS.md: los eventos de junta usan 1000 + meetings.id.
         *
         * La app original generaba los ids de tareas y de juntas en secuencias independientes,
         * por lo que podían solaparse al mezclar ambas listas en el calendario. Desplazar los
         * eventos de junta 1000 posiciones garantiza ids únicos en `GET /calendar/events`;
         * `CalendarService` resta este offset para recuperar el `meetingId` y calcular la
         * asistencia, y `MeetingService` lo suma al sincronizar/create/delete el evento.
         */
        const val MEETING_ID_OFFSET = 1000L
    }
}

/**
 * Capa: entity (enum persistido). Tipos de entrada del calendario de la app.
 *
 * Responsabilidad: distinguir `TASK` (evento con el id de la tarea), `MEETING` (evento con
 * `MEETING_ID_OFFSET + meetings.id`) y `EXAM` (evento creado directamente desde
 * `POST /calendar/events`). Se persiste como texto en `calendar_events.type`.
 *
 * Colaboradores: `CalendarEvent`, `CreateCalendarEventRequest`, `CalendarEventDto` y
 * `CalendarService`. Existe para que el cliente sepa qué icono/detalle mostrar y para decidir
 * cuándo calcula la asistencia derivada.
 */
enum class CalendarEventType { TASK, MEETING, EXAM }
