# Contrato de API — EduvIA

> **Estado:** Fase 1 completada. **Caso B**: la app no consume HTTP todavía; el contrato se deriva de sus modelos de dominio, repositorios y casos de uso.
> Fuente: `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/`.
> Regla: el backend se adapta a los modelos de la app (nombres, nulabilidad, enums, IDs `Long`).

## Convenciones globales

| Concepto | Decisión | Justificación en la app |
|---|---|---|
| Base path | `/api/v1` | Default de la guía para caso B |
| Naming JSON | `camelCase` | Los modelos Kotlin usan camelCase; no hay `@SerialName` ni `namingStrategy` |
| Config Json futura en la app | `ignoreUnknownKeys = true`, `explicitNulls = false` | Añadir en Fase 5 para tolerar campos de control del backend |
| IDs | `Long` (no UUID) | `User.id`, `SchoolTask.id`, `SchoolGroup.id`, `Student.id`, `Meeting.id`, `CalendarEvent.id` son `Long` |
| Fechas de calendario | `YYYY-MM-DD` (`LocalDate` → `date`) | `CalendarDate(year, month, day)` es una fecha sin hora |
| Timestamps | ISO-8601 UTC (`Instant`) | `createdAt`/`updatedAt` del backend; la app no los consume aún |
| Horas | texto libre validado por la app (ej. `8:00 am`) | `Meeting.time: String` |
| Paginación | `PageResponse<T>` `{items, page, size, totalElements, totalPages}` | La app no pagina; se añade a los listados según la guía, `size <= 100` |
| Errores | `ProblemDetail` RFC 9457 (`type`, `title`, `status`, `detail`, `properties.errors`) | Default de la guía; la app solo maneja `AppResult.Error(message)` en Fase 5 |
| Autenticación | Bearer JWT en todos los recursos excepto auth/actuator/swagger | Confirmado por el usuario (Fase 3b) |
| Enums | Literales exactos en mayúsculas | `UserRole`, `Subject`, `TaskStatus`, `MeetingResponse`, `CalendarEventType`, `expenseCategory` |

### Modelos base (schemas compartidos)

- `User { id: Long, fullName: String, email: String, phone: String, role: UserRole }` — `model/User.kt`
- `UserRole`: `PARENT | TEACHER` — `model/RegisterModels.kt`
- `Student { id: Long, name: String, groupId: Long, parentId: Long }` — `model/User.kt`
- `SchoolGroup { id: Long, name: String, school: String, subject: String, code: String, teacherId: Long, studentCount: Int }` — `model/SchoolGroup.kt`
- `ChildInfo { classCode: String, studentName: String }`
- `GroupInput { groupName: String, schoolName: String, subject: String }`
- `CalendarDate` se serializa como **String** `YYYY-MM-DD` (no como objeto `{year,month,day}`); el mapper de la Fase 5 hace la conversión.
- `SchoolTask { id, groupId, title, subject: Subject, description, dueDate: date, publishedDate: date, teacherName, totalStudents: Int, hasPhoto: Boolean, deliveredStudentIds: Long[] }` — `model/SchoolTask.kt`
- `Subject`: `MATEMATICAS | ESPANOL | CIENCIAS | HISTORIA | OTRA`
- `TaskStatus`: `PENDING | DELIVERED | EXPIRED` (calculado; se expone como field derivado en el detalle)
- `Meeting { id, groupId, title, date: date, time: String, location: String, hasVirtualLink: Boolean, isNext: Boolean, relativeLabel: String?, response: MeetingResponse, virtualLink: String?, reminderEnabled: Boolean }` — `model/MeetingModels.kt`
- `MeetingResponse`: `PENDING | ATTENDING | DECLINED`
- `FamilyResponse { familyName: String, response: MeetingResponse, studentId: Long }`
- `AttendanceSummary { confirmed: Int, declined: Int, pending: Int }` (derivado)
- `CalendarEvent { id, groupId, date: date, type: CalendarEventType, title, subtitle, attendance: AttendanceSummary? }`
- `CalendarEventType`: `TASK | MEETING | EXAM`
- `NewTaskInput { title, subject, description, dateText: String }`
- `NewMeetingInput { title, dateText, time, place, link, reminder: Boolean, groupId }`
- `Expense { id, amount: Double, category: expenseCategory, description }` — solo modelo de UI de `ExpenceScreen.kt`, sin repositorio; queda fuera del CRUD (ver OPEN-QUESTIONS).
- `expenseCategory`: `GROCERIES | PARTY | SNACKS | COFFEE | CAR | HOUSE | OTHER`

## Operaciones de la app → endpoints

Todas las rutas llevan el prefijo `/api/v1`. Los endpoints deauth son públicos; el resto requieren `Authorization: Bearer <accessToken>`.

### Autenticación y cuenta (`domain/repository/AuthRepository.kt`, `data/remote/RemoteAuthRepository.kt`, `domain/usecase/AuthUseCases.kt`)

| # | Endpoint | Operación de la app | Request | Response | Errores |
|---|---|---|---|---|---|
| A1 | `POST /auth/register` | `RegisterUseCase` → `AuthRepository.register` + `GroupRepository.createGroup`/`linkStudent` (transacción única) | `RegisterRequest { role, fullName, email, phone, password, confirmPassword, acceptedTerms, children: ChildInfo[], groups: GroupInput[] }` | `201 { user: User, createdGroups: SchoolGroup[], accessToken, refreshToken, expiresIn }` | 400 validación (nombre/email/teléfono/contraseña/términos), 409 email o teléfono duplicado, 400 código de clase inexistente |
| A2 | `POST /auth/login` | `LoginUseCase` → `AuthRepository.login` | `LoginRequest { email, password }` | `200 { user: User, accessToken, refreshToken, expiresIn }` | 400 email inválido o contraseña vacía, 401 credenciales inválidas |
| A3 | `POST /auth/refresh` | Renovación Bearer (Fase 3b) | `RefreshRequest { refreshToken }` | `200 { accessToken, refreshToken, expiresIn }` (rotación) | 401 refresh inválido/revocado |
| A4 | `POST /auth/logout` | `LogoutUseCase` | `RefreshRequest { refreshToken }` | `204` (revoca) | 401 |
| A5 | `GET /auth/exists/email?email=` | `AuthRepository.emailExists` (validación paso 1) | query | `200 { exists: Boolean }` | 400 |
| A6 | `GET /auth/exists/phone?phone=` | `AuthRepository.phoneExists` | query | `200 { exists: Boolean }` | 400 |
| A7 | `POST /auth/reset-code` | `SendResetCodeUseCase` → `createResetCode` + `FakeEmailSender` | `{ email }` | `200 { }` (siempre, para no revelar si existe) | 400 |
| A8 | `POST /auth/reset-password` | `ResetPasswordUseCase` | `{ email, code, newPassword, confirmPassword }` | `200 { }` | 400 formato, 400 código incorrecto/vencido/agotado |
| A9 | `GET /me` | `ObserveSessionUseCase` | — | `200 User` | 401 |

> Nota A7: en `dev` el código de 6 dígitos se devuelve en la respuesta (`resetCode`) para alimentar el `demoInbox` de la app; en `prod` se omite. Ver OPEN-QUESTIONS.

### Grupos y alumnos (`domain/repository/GroupRepository.kt`)

| # | Endpoint | Operación de la app | Query/Body | Response | Errores |
|---|---|---|---|---|---|
| G1 | `GET /groups?teacherId=` | `observeGroupsOf(teacherId)` | query `teacherId` | `200 PageResponse<SchoolGroup>` | 401, 400 |
| G2 | `GET /groups/{id}` | `findById(id)` | — | `200 SchoolGroup` | 404 |
| G3 | `GET /groups/by-code?code=` | `findByCode(code)` | query `code` | `200 SchoolGroup` | 404 |
| G4 | `POST /groups` | `createGroup(teacherId, input)` | `{ teacherId, groupName, schoolName, subject }` | `201 SchoolGroup` (code generado `XX-YYYY` único) | 400, 401, 409 código duplicado |
| G5 | `GET /groups/{id}/students` | `observeStudentsOfGroup(groupId)` | — | `200 PageResponse<Student>` | 404 |
| G6 | `GET /students?parentId=` | `observeStudentsOf(parentId)` | query `parentId` | `200 PageResponse<Student>` | 400 |
| G7 | `POST /students:link` | `linkStudent(parentId, child)` | `{ parentId, classCode, studentName }` | `201 Student` (incrementa `studentCount` del grupo) | 400 nombre vacío, 404 código inexistente, 409 |

### Tareas (`domain/repository/TaskRepository.kt`, `domain/usecase/TaskUseCases.kt`)

| # | Endpoint | Operación de la app | Query/Body | Response | Errores |
|---|---|---|---|---|---|
| T1 | `GET /tasks?groupId=&page=&size=` | `observeTasks(groupId)` (orden por `dueDate`) | query | `200 PageResponse<SchoolTask>` | 400, 401 |
| T2 | `GET /tasks/{id}` | detalle de tarea | — | `200 SchoolTask` (+ `status` calculado) | 404 |
| T3 | `POST /tasks` | `CreateTaskUseCase` → `addTask` + `calendar.addEvent` (misma transacción) | `{ groupId, title, subject, description, dateText }` | `201 SchoolTask` | 400 título/fecha, 404 grupo, 401 |
| T4 | `PUT /tasks/{id}` | `UpdateTaskUseCase` → `updateTask` | `{ title, subject, description, dateText }` | `200 SchoolTask` | 400, 404 |
| T5 | `DELETE /tasks/{id}` | `deleteTask(taskId)` | — | `204` | 404 |
| T6 | `POST /tasks/{id}/deliveries` | `markDelivered(taskId, studentId)` (idempotente) | `{ studentId }` | `200 SchoolTask` (con `deliveredStudentIds` actualizado) | 404, 400, 409 si el alumno no pertenece al grupo |

### Juntas (`domain/repository/MeetingRepository.kt`, `domain/usecase/MeetingUseCases.kt`)

| # | Endpoint | Operación de la app | Query/Body | Response | Errores |
|---|---|---|---|---|---|
| M1 | `GET /meetings?page=&size=` | `observeMeetings()` (orden por fecha; `isNext`/`relativeLabel` calculados) | query | `200 PageResponse<Meeting>` | 401 |
| M2 | `GET /meetings/{id}` | detalle | — | `200 Meeting` | 404 |
| M3 | `POST /meetings` | `CreateMeetingUseCase` → `addMeeting` + `calendar.addEvent` | `NewMeetingInput` | `201 Meeting` | 400 título/fecha/hora/lugar/URL, 409 junta duplicada en fecha+hora+lugar+grupo |
| M4 | `DELETE /meetings/{id}` | `deleteMeeting(meetingId)` | — | `204` | 404 |
| M5 | `GET /meetings/{id}/attendance` | `observeAttendance(meetingId)` | — | `200 PageResponse<FamilyResponse>` | 404 |
| M6 | `PUT /meetings/{id}/responses/{studentId}` | `setResponse(meetingId, studentId, response)` | `{ response: MeetingResponse }` | `200 Meeting` | 400, 404 |
| M7 | `GET /meetings/responses?studentId=` | `observeResponses(studentId)` → `Map<meetingId, response>` | query | `200 { responses: { "1": "ATTENDING", ... } }` | 400 |
| M8 | `POST /meetings/{id}/reminders` | `remindPending(meetingId): Int` | — | `200 { pending: Int }` | 404 |
| M9 | `PUT /meetings/{id}` | `UpdateMeetingUseCase` → `updateMeeting` + `calendar.addEvent` (mismo id `1000+id`) | `{ title, dateText, time, place, link?, reminder }` | `200 Meeting` | 400 título/fecha/hora/lugar/URL, 404, 409 junta duplicada en fecha+hora+lugar+grupo (excluyendo la propia) |

### Calendario (`domain/repository/CalendarRepository.kt`)

| # | Endpoint | Operación de la app | Query/Body | Response | Errores |
|---|---|---|---|---|---|
| C1 | `GET /calendar/events?role=PARENT|TEACHER&page=&size=` | `observeEvents(role)` — la vista del docente incluye `attendance`, la del padre no | query | `200 PageResponse<CalendarEvent>` | 400 role, 401 |
| C2 | `POST /calendar/events` | `addEvent(event)` (también lo hacen T3 y M3 internamente) | `CalendarEvent` (sin `attendance`) | `201 CalendarEvent` | 400 |
| C3 | `DELETE /calendar/events/{id}` | `deleteEvent(eventId)` | — | `204` | 404 |

## Reglas de negocio que el backend debe replicar

1. **Registro atómico**: crear usuario + grupos (docente) o vincular alumnos (padre) en una sola transacción; si falla un código de clase, no queda usuario creado.
2. **Códigos de grupo** `XX-YYYY` (alfabeto sin ambigüedades), únicos, case-insensitive en búsqueda.
3. **Tarea → calendario**: crear tarea crea un `CalendarEvent` de tipo `TASK` con el **mismo id** que la tarea; borrar tarea borra ese evento.
4. **Junta → calendario**: crear junta crea un `CalendarEvent` de tipo `MEETING` con id `1000 + meetings.id` (las juntas y las tareas tenían ids solapados en la app); editar junta actualiza ese mismo evento; borrar junta borra ese evento. Ver `DECISIONS.md` y `data-model.md`.
5. **Unicidad de junta**: no dos juntas para el mismo `groupId` + `date` + `time` + `location` → 409.
6. **Entregas idempotentes**: marcar entregada dos veces no duplica el alumno en `deliveredStudentIds`.
7. **Asistencia derivada**: `AttendanceSummary` se calcula de las respuestas, nunca se almacena.
8. **Duplicidad de cuenta**: email y teléfono normalizados (email lower-case, teléfono solo dígitos), únicos → 409.
9. **Códigos de reset**: 6 dígitos, 10 min de vigencia, 5 intentos, un solo uso.

## Fuera del contrato (anotado, no implementado)

- `expenseCategory` / `Expense`: solo pantalla `ExpenceScreen.kt`, sin repositorio ni caso de uso → no se crea endpoint (ver OPEN-QUESTIONS).
- `SchoolTask.displayTitle`, `deliveredCount`, `missingCount`, `isComplete`, `deliveredFraction`: propiedades calculadas de la app → se calculan en el mapper de la Fase 5, no se almacenan.
- `Meeting.isNext`/`relativeLabel` y `AttendanceSummary.total`/`confirmedFraction`: derivados; el backend puede enviarlos como conveniencia pero la app los recalcula.

## Verificación del escaneo

- Llamadas HTTP encontradas por `scan-kmp.ps1` **en Fase 1: 0** → caso B confirmado. Tras la Fase 5 el mismo escaneo detecta Ktor 3.2.0, 31 `@Serializable` y 8 repositorios remotos; el contrato no cambió, solo se conectó la app (ver `compat-report.md`). En Fase 6 Ktor se actualizó a 3.2.1 y se añadió M9 (`PUT /meetings/{id}`).
- Repositorios analizados: 5 (`Auth`, `Group`, `Task`, `Meeting`, `Calendar`).
- Operaciones de repositorio: 8 + 7 + 5 + 8 + 3 = **31** → 31 endpoint-filas en este contrato (incluye auth/refresh/logout/exists/reset/me y los derivados de calendario).
- Cada operación tiene fila en `docs/backend/test-plan.md`.
