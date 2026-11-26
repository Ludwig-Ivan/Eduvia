# Plan de pruebas — EduvIA

> Una fila por endpoint del contrato. Las casillas se marcan cuando la prueba correspondiente está en verde.
> Tipos: **U** unitaria · **W** web slice · **I** integración (Testcontainers) · **C** contrato (fixture) · **E** E2E (Docker) · **A** app KMP (MockEngine/fixture).

## Autenticación

| Id | Endpoint | Escenario | Tipo | Estado |
|---|---|---|---|---|
| A1-ok | POST /auth/register | Docente con 2 grupos → 201, user+groups+tokens; grupos con código único | I, C | [x] |
| A1-parent | POST /auth/register | Padre con hijo y código válido → 201, alumno vinculado y `studentCount` incrementado | I | [x] |
| A1-val | POST /auth/register | Nombre/email/teléfono/contraseña/términos inválidos → 400 con `properties.errors` | W | [x] |
| A1-dup | POST /auth/register | Email o teléfono duplicado → 409 | I | [ ] |
| A1-code | POST /auth/register | Código de clase inexistente → 400 y **sin usuario creado** (atomicidad) | I | [x] |
| A2-ok | POST /auth/login | Credenciales demo (`laura@eduvia.mx`) → 200 con tokens | I, C, E | [x] |
| A2-bad | POST /auth/login | Contraseña incorrecta → 401 | I | [x] |
| A2-val | POST /auth/login | Email mal formado / contraseña vacía → 400 | W | [ ] |
| A3-ok | POST /auth/refresh | Refresh válido → 200, token anterior revocado (rotación) | I | [x] |
| A3-bad | POST /auth/refresh | Refresh revocado o expirado → 401 + `WWW-Authenticate: Bearer` | I | [x] |
| A4-ok | POST /auth/logout | 204 y refresh revocado | I | [x] |
| A5-ok | GET /auth/exists/email | exists=true / false | I | [x] |
| A6-ok | GET /auth/exists/phone | Normalización solo dígitos; true / false | I | [x] |
| A7-ok | POST /auth/reset-code | Correo existente → 200; correo inexistente → 200 idéntico | I | [ ] |
| A8-ok | POST /auth/reset-password | Código válido → 200; reutilización → 400 | I | [x] |
| A8-bad | POST /auth/reset-password | Código erróneo 5 veces → 400 "Demasiados intentos" | U | [ ] |
| A9-ok | GET /me | Bearer válido → 200 User; sin token → 401 | I | [x] |
| AUTH-sec | Varios | Sin token → 401 + header Bearer; token expirado → 401; rol insuficiente → 403 | I | [ ] |

## Grupos y alumnos

| Id | Endpoint | Escenario | Tipo | Estado |
|---|---|---|---|---|
| G1-ok | GET /groups?teacherId=1 | 200 `PageResponse`, items shape SchoolGroup | I, C | [x] |
| G1-page | GET /groups?teacherId=1&page=0&size=1 | Paginación, `totalPages`, `size<=100`, orden estable | I | [x] |
| G2-ok | GET /groups/{id} | 200 | I | [ ] |
| G2-404 | GET /groups/{id} | 404 ProblemDetail | I | [x] |
| G3-ok | GET /groups/by-code?code=3b-7k2p | Búsqueda case-insensitive → 200 | I | [x] |
| G4-ok | POST /groups | Código `XX-YYYY` único → 201 | I, C | [x] |
| G4-val | POST /groups | groupName/schoolName vacíos → 400 | W | [ ] |
| G5-ok | GET /groups/{id}/students | 200 PageResponse<Student> | I | [x] |
| G6-ok | GET /students?parentId=2 | 200, solo los alumnos del padre | I | [x] |
| G7-ok | POST /students:link | 201, `studentCount+1` | I, C | [x] |
| G7-bad | POST /students:link | Código inexistente → 404; nombre vacío → 400 | I | [ ] |
| G7-dup | POST /students:link | Mismo alumno dos veces → 409 | I | [ ] |

## Tareas

| Id | Endpoint | Escenario | Tipo | Estado |
|---|---|---|---|---|
| T1-ok | GET /tasks?groupId=1 | 200 ordenado por `dueDate`; shape con fechas `YYYY-MM-DD` | I, C, E, A | [x] |
| T1-page | GET /tasks?groupId=1&page=&size= | Paginación y límite `size<=100` | I | [x] |
| T2-ok | GET /tasks/{id} | 200 con `subject` enum y `deliveredStudentIds: number[]` | I, C | [x] |
| T2-404 | GET /tasks/{id} | 404 | I | [x] |
| T3-ok | POST /tasks | 201 + evento de calendario con el mismo id (transacción) | I, C, E | [x] |
| T3-val | POST /tasks | Título vacío / fecha pasada / formato inválido → 400 | W, I | [x] |
| T3-404 | POST /tasks | groupId inexistente → 404 | I | [ ] |
| T4-ok | PUT /tasks/{id} | 200 con campos actualizados | I | [x] |
| T4-val | PUT /tasks/{id} | Título vacío, fecha inválida → 400; id inexistente → 404 | I | [ ] |
| T5-ok | DELETE /tasks/{id} | 204; borra evento `TASK` asociado; segundo delete → 404 | I, E | [x] |
| T6-ok | POST /tasks/{id}/deliveries | 200, idempotente (dos veces = mismo set) | I | [x] |
| T6-bad | POST /tasks/{id}/deliveries | Tarea inexistente → 404; alumno ajeno al grupo → 409 | I | [x] |
| T-n1 | GET /tasks | Sin N+1: consultas acotadas con estadísticas de Hibernate | I | [ ] |

## Juntas

| Id | Endpoint | Escenario | Tipo | Estado |
|---|---|---|---|---|
| M1-ok | GET /meetings | 200 ordenado; `isNext`/`relativeLabel` calculados | I, C, A | [x] |
| M2-ok | GET /meetings/{id} | 200; `virtualLink:null` y `relativeLabel:null` presentes cuando aplica | I, C | [ ] |
| M3-ok | POST /meetings | 201 + evento `MEETING` con id `1000 + meeting.id` | I, C, E | [x] |
| M3-val | POST /meetings | Título, fecha pasada, hora inválida, lugar vacío, URL inválida → 400 | W, I | [x] |
| M3-conf | POST /meetings | Misma fecha+hora+lugar+grupo → 409 | I | [x] |
| M4-ok | DELETE /meetings/{id} | 204; borra evento y respuestas; 404 después | I | [x] |
| M5-ok | GET /meetings/{id}/attendance | 200 `FamilyResponse[]` derivado de alumnos del grupo | I, C | [x] |
| M6-ok | PUT /meetings/{id}/responses/{studentId} | 200; respuesta sobreescribible | I | [x] |
| M6-bad | PUT /meetings/{id}/responses/{studentId} | response inválido → 400; meeting inexistente → 404 | I | [ ] |
| M7-ok | GET /meetings/responses?studentId=1 | 200 mapa `{ "1":"ATTENDING" }` | I | [x] |
| M8-ok | POST /meetings/{id}/reminders | 200 `{pending: N}` coherente con attendance | I | [x] |
| M9-ok | PUT /meetings/{id} | 200; conserva id y actualiza evento `1000+id` | I, A | [x] |
| M9-conf | PUT /meetings/{id} | Misma fecha+hora+lugar+grupo (excluyendo la propia junta) → 409 | I | [x] |

## Calendario

| Id | Endpoint | Escenario | Tipo | Estado |
|---|---|---|---|---|
| C1-parent | GET /calendar/events?role=PARENT | 200, `attendance: null` en todos | I, C | [x] |
| C1-teacher | GET /calendar/events?role=TEACHER | 200, `attendance` con confirmed/declined/pending en juntas | I, C | [x] |
| C1-val | GET /calendar/events?role=BAD | 400 | W | [x] |
| C2-ok | POST /calendar/events | 201 | I | [x] |
| C3-ok | DELETE /calendar/events/{id} | 204 y 404 después | I | [x] |

## BD, contrato y arquitectura

| Id | Escenario | Tipo | Estado |
|---|---|---|---|
| BD1 | Todas las migraciones se aplican sobre Postgres limpio; `flyway.info().pending()` vacío | I | [x] |
| BD2 | `information_schema`: tablas/columnas/tipos esperados | I | [ ] |
| BD3 | Cada `unique`, `check`, `not null` y FK rechaza datos inválidos | I | [ ] |
| BD4 | Contexto arranca con `ddl-auto: validate` (entidades == Flyway) | I | [x] |
| FX1 | `contract/fixtures/*.json` generados para login, register, groups, tasks, meetings, calendar | C | [x] |
| FX2 | La app deserializa cada fixture con `Json { ignoreUnknownKeys = false }` | A | [x] |
| SYNC1 | Capa de red KMP compila en `commonMain`, Android e iOS (`compile*`) | A | [x] |
| SYNC2 | `ApiClientTest`: ruta/query/token, refresco 401, `ProblemDetail`, respuestas 204 | A | [x] |
| SYNC3 | `RemoteRepositoriesTest`: mapeo DTO→dominio y bodies exactos del contrato | A | [x] |
| SYNC4 | Base URL por plataforma y HTTP local (Android `10.0.2.2`, iOS `localhost`, ATS) | A | [x] |
| SYNC5 | `scan-kmp.ps1` tras Fase 5 detecta Ktor, `@Serializable` y repositorios remotos | C, A | [x] |
| OC1 | `/v3/api-docs` contiene todos los paths/métodos de `openapi.yaml` | I | [x] |
| COV | `gradlew.bat check` con cobertura JaCoCo ≥ 80 % | — | [x] |
| E2E1 | Stack Docker: health UP, login + CRUD completo persiste tras reinicio de `api` | E | [x] |
| ALL | `scripts\test-all.ps1` desde cero termina con ✅ | — | [x] |

**Total de escenarios planificados:** 31 endpoints × casos + 15 transversales (BD/contrato/arquitectura/sync de la app KMP).
