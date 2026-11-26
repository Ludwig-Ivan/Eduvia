# Informe de compatibilidad app KMP ↔ API — EduvIA

> Generado en la Fase 5 con la skill `kmp-client-sync`. Fuentes comparadas:
> - App KMP: `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/` (31 clases `@Serializable`, 8 repositorios remotos).
> - Backend en ejecución: `GET http://localhost:18083/v3/api-docs` (springdoc, stack aislado `miapp-compat`).
> - Contrato de diseño: `docs/backend/openapi.yaml` y respuestas reales en `contract/fixtures/*.json`.

## Resumen

| Aspecto | Estado | Detalle |
|---|---|---|
| Rutas y operaciones | ✅ Compatible | 26 paths / 34 operaciones en diseño y en runtime; `missing=[]`, `extra=[]` |
| DTOs y campos | ✅ Compatible | 35 schemas runtime; nombre y tipo de cada campo coinciden con los DTOs de la app y con los 11 fixtures |
| Nulabilidad | ✅ Compatible | `parentId` BD `NULL` ↔ app `0`; `attendance: null` para rol PARENT; opcionales con default en la app |
| Enums | ✅ Compatible | `UserRole`, `Subject`, `TaskStatus`, `MeetingResponse`, `CalendarEventType` con los mismos literales |
| Fechas | ✅ Compatible | JSON `YYYY-MM-DD`; la app convierte a/from `CalendarDate`; `dateText` ISO en ambas direcciones |
| Errores | ✅ Compatible | `application/problem+json` (RFC 9457) con `detail`, `status`, `title`, `instance`, `errors`; la app parsea `ProblemDto` |
| Paginación | ✅ Compatible | `items`, `page`, `size`, `totalElements`, `totalPages` = `PageDto<T>` de la app |
| Autenticación | ✅ Compatible | `Authorization: Bearer`; 401 + `WWW-Authenticate: Bearer`; refresco automático y reintento verificados con `MockEngine` |
| Códigos de estado | ✅ Compatible | 200/201/204/400/401/404/409 confirmados por tests I/E y por peticiones reales al stack (ver abajo) |
| Pruebas de contrato en la app | ✅ Compatible | `ContractFixturesFileTest` deserializa los 11 fixtures con `ignoreUnknownKeys = false` |

No hay incompatibilidades abiertas que requieran cambios en el backend o en la app.

## Checklist por endpoint

| Endpoint | Estado | Problema | Lado a corregir | Propuesta |
|---|---|---|---|---|
| `POST /api/v1/auth/register` | ✅ | — | — | `RegisterRequest` / `RegistrationResponse` coinciden; `children` y `groups` opcionales |
| `POST /api/v1/auth/login` | ✅ | — | — | `LoginRequest` y `TokenResponse` con `user`, `createdGroups`, tokens y `expiresIn` |
| `POST /api/v1/auth/refresh` | ✅ | — | — | Rotación de refresh; la app reintenta la petición original tras 401 |
| `POST /api/v1/auth/logout` | ✅ | — | — | Sin cuerpo de respuesta; `postNoContent` acepta 200/204 |
| `GET /api/v1/auth/exists/email` | ✅ | — | — | `ExistsResponse { exists }` |
| `GET /api/v1/auth/exists/phone` | ✅ | — | — | Normalización a solo dígitos en backend y app |
| `POST /api/v1/auth/reset-code` | ✅ | — | — | `ResetCodeResponse { resetCode }` en perfil `dev` |
| `POST /api/v1/auth/reset-password` | ✅ | — | — | `ResetPasswordRequest` coincide; errores como `ProblemDetail` |
| `GET /api/v1/me` | ✅ | — | — | Devuelve `UserDto` con `role` enum en texto |
| `GET /api/v1/groups` | ✅ | — | — | `PageResponseSchoolGroupDto`; query `teacherId`, `page`, `size` |
| `POST /api/v1/groups` | ✅ | — | — | `CreateGroupRequest` coincide con `CreateGroupRequestDto` |
| `GET /api/v1/groups/{id}` | ✅ | — | — | `SchoolGroupDto`; 404 como `ProblemDetail` |
| `GET /api/v1/groups/by-code` | ✅ | — | — | Búsqueda case-insensitive; 404 como `ProblemDetail` |
| `GET /api/v1/groups/{id}/students` | ✅ | — | — | `PageResponseStudentDto` |
| `GET /api/v1/students` | ✅ | — | — | Query `parentId`; `parentId` nullable en BD se mapea a `0` |
| `POST /api/v1/students:link` | ✅ | — | — | `LinkStudentRequest` coincide; ruta con dos puntos literal |
| `GET /api/v1/tasks` | ✅ | — | — | `PageResponseSchoolTaskDto`; fechas `YYYY-MM-DD` |
| `POST /api/v1/tasks` | ✅ | — | — | `CreateTaskRequest.dateText = YYYY-MM-DD`; 201 y evento `TASK` del mismo id |
| `GET /api/v1/tasks/{id}` | ✅ | — | — | `deliveredStudentIds` como `number[]` (`List<Long>`) |
| `PUT /api/v1/tasks/{id}` | ✅ | — | — | `UpdateTaskRequest` coincide |
| `DELETE /api/v1/tasks/{id}` | ✅ | — | — | 204; borra el evento `TASK` asociado |
| `POST /api/v1/tasks/{id}/deliveries` | ✅ | — | — | `MarkDeliveredRequest { studentId }`; idempotente |
| `GET /api/v1/meetings` | ✅ | — | — | `isNext`/`relativeLabel` calculados por el backend y tolerados por la app |
| `POST /api/v1/meetings` | ✅ | — | — | Evento `MEETING` con id `1000 + meetings.id` |
| `GET /api/v1/meetings/{id}` | ✅ | — | — | Campos nulos explícitos presentes; `ignoreUnknownKeys=false` pasa |
| `PUT /api/v1/meetings/{id}` | ✅ | — | — | Añadido en Fase 6 para edición real; conserva el id y actualiza el evento `1000+id` |
| `DELETE /api/v1/meetings/{id}` | ✅ | — | — | 204; borra evento y respuestas |
| `GET /api/v1/meetings/{id}/attendance` | ✅ | — | — | `FamilyResponseDto[]` derivado de los alumnos del grupo |
| `PUT /api/v1/meetings/{id}/responses/{studentId}` | ✅ | — | — | `SetResponseRequest.response` usa `PENDING/ATTENDING/DECLINED` |
| `GET /api/v1/meetings/responses` | ✅ | — | — | Mapa `{ "id": "RESPONSE" }` = `Map<String, String>` |
| `POST /api/v1/meetings/{id}/reminders` | ✅ | — | — | `PendingDto { pending }` |
| `GET /api/v1/calendar/events` | ✅ | — | — | `CalendarEventDto.attendance` nulo para PARENT |
| `POST /api/v1/calendar/events` | ✅ | — | — | `CreateCalendarEventRequest` coincide; la app solo lo usa para `EXAM` |
| `DELETE /api/v1/calendar/events/{id}` | ✅ | — | — | 204; `deleteNoContent` de la app lo maneja |

## Evidencia de ejecución real (Fase 5)

- Rescan de la app: `powershell -NoProfile -ExecutionPolicy Bypass -File ..\.opencode\skills\kmp-api-contract\scripts\scan-kmp.ps1 .`
  → Ktor 3.2.1 (actualizado desde 3.2.0 en Fase 6 por un fallo de D8; ver `PROGRESS.md`) + logging + content-negotiation + motores OkHttp/Darwin, 31 `@Serializable`, 8 repositorios remotos, refresh 401 y parseo de ProblemDetail.
- Compilaciones: `:shared:compileCommonMainKotlinMetadata`, `:shared:compileAndroidMain`, `:shared:compileKotlinIosArm64`, `:shared:compileTestKotlinIosArm64` → `BUILD SUCCESSFUL`.
- Pruebas: `.\gradlew.bat --no-daemon :shared:testAndroid` → `BUILD SUCCESSFUL`; 17 tests, 0 failures, 0 skipped (5 clases: `ContractFixturesFileTest`, `ApiClientTest`, `RemoteRepositoriesTest`, `SharedCommonTest`, `SharedLogicAndroidHostTest`).
- Contrato runtime vs diseño (node + `/v3/api-docs`): `design=34 runtime=34`, `missing=[]`, `extra=[]`, 26 paths y 36 schemas (tras añadir `PUT /meetings/{id}` en Fase 6).
- Peticiones reales al stack `miapp-compat` (API en `18083`):
  - `POST /api/v1/auth/login` → `200`, JWT HS256 (`sub`, `roles`, `iss`), `expiresIn=900`.
  - `POST /api/v1/tasks` con `dateText=2026-12-19` → `201` (id 7); `DELETE /api/v1/tasks/7` → `204`.
  - `GET /api/v1/groups/999999` → `404`, `Content-Type: application/problem+json`, cuerpo `{"detail":"Grupo no encontrado","status":404,"title":"Not Found","errors":[]}`.
  - `GET /api/v1/me` sin token → `401`, `WWW-Authenticate: Bearer`, `application/problem+json`.

## Limitaciones documentadas (no son incompatibilidades)

1. La sesión de la app se guarda en memoria (`SessionStore`); no hay persistencia segura de tokens todavía.
2. `RegisterUseCase` no envía hijos/grupos en el registro (envía `[]`); la vinculación se hace después con `POST /students:link` y `POST /groups`.
3. springdoc declara `200` de forma genérica en `/v3/api-docs`; los códigos reales (201/204/404/409) están verificados por las pruebas de integración/E2E y por las peticiones reales anteriores.
4. Las pruebas del simulador iOS no se ejecutan en Windows; la compilación `iosArm64` (main + test) sí se verifica aquí. La prueba manual en dispositivo/emulador queda para la Fase 6.
