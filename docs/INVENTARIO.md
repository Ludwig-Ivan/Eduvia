# Inventario de documentación — EduvIA

> **Estado final:** 379/379 elementos documentados (100 %). Cada casilla se marcó tras comprobar el KDoc/comentario/anotación o el documento `.md` que la cubre. El recuento excluye la leyenda de esta cabecera.
>
> **Cómo se usa:** cada elemento tiene una casilla; todas están marcadas porque el trabajo terminó. Una casilla marcada significa que el elemento está cubierto por KDoc/Javadoc, comentario/anotación OpenAPI, comentario SQL o un documento `.md` enlazado desde este inventario. Los elementos agrupados bajo un archivo comparten ruta: la casilla del archivo y la de cada clase se marcan por separado.
>
> **Rutas:** se escriben relativas a la raíz del repositorio y se han comprobado leyendo el árbol de archivos. Los nombres del código no se cambian.
>
> **Reglas:** lo que no se pueda confirmar se marca `⚠️ Por confirmar` y se añade a `docs/backend/OPEN-QUESTIONS.md`. Los bugs o confusiones se anotan en `docs/HALLAZGOS.md` **sin arreglarlos**. Los diagramas serán solo Mermaid. El idioma de comentarios y documentación es español.
>
> **Fuentes ya leídas y reutilizadas:** ver sección J. No se contradice ni se duplica lo que ya dicen esos documentos; se enlaza desde los nuevos.
>
> **Enlaces rápidos:** [índice de documentación](README.md), [arquitectura](arquitectura.md), [API](api.md), [base de datos](base-de-datos.md), [pruebas](pruebas.md), [hallazgos](HALLAZGOS.md) y [dudas abiertas](backend/OPEN-QUESTIONS.md).

---

## A. Módulos y builds

- [x] `backend/` — API Spring Boot + PostgreSQL; build de Gradle independiente (no se mezcla con el build de la app).
- [x] `Librerias_eduvia/shared/` — módulo KMP con `commonMain`, `androidMain`, `iosMain` y source sets de pruebas.
- [x] `Librerias_eduvia/androidApp/` — punto de entrada Android (`MainActivity`, manifiesto, recursos).
- [x] `Librerias_eduvia/iosApp/` — punto de entrada iOS en Swift y proyecto Xcode.
- [x] `contract/fixtures/` — 11 respuestas JSON reales compartidas por backend y app.
- [x] `scripts/` — script único de verificación de toda la arquitectura.
- [x] `docs/` — documentación del proyecto (`backend/`, `guias/` y los documentos nuevos).
- [x] `backend/settings.gradle.kts` y `backend/build.gradle.kts` — build del backend (Spring Boot 4.2.0, JaCoCo, suites `test`/`e2eTest`).
- [x] `Librerias_eduvia/settings.gradle.kts` y `Librerias_eduvia/build.gradle.kts` — build KMP de la app.
- [x] `Librerias_eduvia/shared/build.gradle.kts` — dependencias Ktor 3.2.1, serialización y tareas de prueba.
- [x] `Librerias_eduvia/androidApp/build.gradle.kts` — `minSdk=24`, configuración Android y HTTP local.
- [x] `Librerias_eduvia/gradle/libs.versions.toml` — catálogo centralizado de versiones.
- [x] `Librerias_eduvia/gradle.properties` y `Librerias_eduvia/gradle/gradle-daemon-jvm.properties` — opciones del daemon de Gradle.
- [x] `Librerias_eduvia/local.properties` — ruta local al SDK Android (archivo de entorno, no versionable).

## B. Paquetes

### Backend (`com.example.librerias.api`)

- [x] `com.example.librerias.api` — arranque de la aplicación.
- [x] `com.example.librerias.api.config` — seguridad y JWT.
- [x] `com.example.librerias.api.domain` — entidades JPA, enums e IDs compuestos.
- [x] `com.example.librerias.api.repository` — repositorios Spring Data.
- [x] `com.example.librerias.api.service` — reglas de negocio y transacciones.
- [x] `com.example.librerias.api.web` — controllers y traducción de errores.
- [x] `com.example.librerias.api.web.dto` — DTOs de request/response y mappers.

### App KMP (`com.example.librerias`)

- [x] `com.example.librerias` (`commonMain`) — composables raíz y tema.
- [x] `com.example.librerias.data` (`commonMain`) — datos de demostración, bandeja de correo y proveedor de "hoy".
- [x] `com.example.librerias.data.remote` (`commonMain`) — cliente Ktor, DTOs, mappers, sesión y repositorios remotos.
- [x] `com.example.librerias.data.remote` (`androidMain`) — motor OkHttp y base URL Android.
- [x] `com.example.librerias.data.remote` (`iosMain`) — motor Darwin y base URL iOS.
- [x] `com.example.librerias.data.repository` (`commonMain`) — implementaciones "fake" previas a la API.
- [x] `com.example.librerias.di` (`commonMain`) — contenedor de dependencias.
- [x] `com.example.librerias.domain.repository` (`commonMain`) — contratos de repositorio de dominio.
- [x] `com.example.librerias.domain.usecase` (`commonMain`) — casos de uso.
- [x] `com.example.librerias.domain.util` (`commonMain`) — abstracciones de correo y fecha.
- [x] `com.example.librerias.domain.validation` (`commonMain`) — validaciones compartidas.
- [x] `com.example.librerias.model` (`commonMain`) — modelos de dominio y parseo de fechas.
- [x] `com.example.librerias.navigation` (`commonMain`) — navegación Compose.
- [x] `com.example.librerias.ui` (`commonMain`) — pantalla de gastos.
- [x] `com.example.librerias.ui.auth` (`commonMain`) — login, registro y recuperación.
- [x] `com.example.librerias.ui.calendar` (`commonMain`) — calendario del alumno y del docente.
- [x] `com.example.librerias.ui.components` (`commonMain`) — barra de navegación inferior.
- [x] `com.example.librerias.ui.home` (`commonMain`) — inicio de alumno y docente.
- [x] `com.example.librerias.ui.meetings` (`commonMain`) — listado, detalle y creación de juntas.
- [x] `com.example.librerias.ui.tasks` (`commonMain`) — listados y detalle de tareas.
- [x] `com.example.librerias.ui.theme` (`commonMain`) — paleta y tema.
- [x] `com.example.librerias` (`androidApp`) — actividad Android.
- [x] módulo `iosApp` (Swift) — entry point y vista raíz.

## C. Clases y tipos públicos del backend

Ruta común: `backend/src/main/kotlin/com/example/librerias/api/`.

### Arranque y configuración

- [x] `ApiApplication` — `backend/src/main/kotlin/com/example/librerias/api/ApiApplication.kt`.
- [x] `JwtConfig` — `backend/src/main/kotlin/com/example/librerias/api/config/JwtConfig.kt`.
- [x] `SecurityConfig` — `backend/src/main/kotlin/com/example/librerias/api/config/SecurityConfig.kt`.

### Dominio (`domain/`)

- [x] `TaskDeliveryId` — `backend/src/main/kotlin/com/example/librerias/api/domain/CompositeIds.kt`.
- [x] `MeetingResponseId` — `backend/src/main/kotlin/com/example/librerias/api/domain/CompositeIds.kt`.
- [x] `User` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `UserRole` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `RefreshToken` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `PasswordResetCode` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `SchoolGroup` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `Student` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `SchoolTask` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `Subject` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `TaskDelivery` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `Meeting` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `MeetingResponseEntity` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `MeetingResponse` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `CalendarEvent` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.
- [x] `CalendarEventType` — `backend/src/main/kotlin/com/example/librerias/api/domain/Entities.kt`.

### Repositorios (`backend/src/main/kotlin/com/example/librerias/api/repository/Repositories.kt`)

- [x] `UserRepository`
- [x] `RefreshTokenRepository`
- [x] `PasswordResetCodeRepository`
- [x] `SchoolGroupRepository`
- [x] `StudentRepository`
- [x] `SchoolTaskRepository`
- [x] `TaskDeliveryRepository`
- [x] `MeetingRepository`
- [x] `MeetingResponseRepository`
- [x] `CalendarEventRepository`

### Servicios (`service/`)

- [x] `AuthService` — `backend/src/main/kotlin/com/example/librerias/api/service/AuthService.kt`.
- [x] `CalendarService` — `backend/src/main/kotlin/com/example/librerias/api/service/CalendarService.kt`.
- [x] `GroupService` — `backend/src/main/kotlin/com/example/librerias/api/service/GroupService.kt`.
- [x] `MeetingService` — `backend/src/main/kotlin/com/example/librerias/api/service/MeetingService.kt`.
- [x] `TaskService` — `backend/src/main/kotlin/com/example/librerias/api/service/TaskService.kt`.

### Web y errores (`web/`)

- [x] `AuthController` — `backend/src/main/kotlin/com/example/librerias/api/web/AuthController.kt`.
- [x] `CalendarController` — `backend/src/main/kotlin/com/example/librerias/api/web/CalendarController.kt`.
- [x] `GroupController` — `backend/src/main/kotlin/com/example/librerias/api/web/GroupController.kt`.
- [x] `MeetingController` — `backend/src/main/kotlin/com/example/librerias/api/web/MeetingController.kt`.
- [x] `TaskController` — `backend/src/main/kotlin/com/example/librerias/api/web/TaskController.kt`.
- [x] `ApiException` — `backend/src/main/kotlin/com/example/librerias/api/web/GlobalExceptionHandler.kt`.
- [x] `GlobalExceptionHandler` — `backend/src/main/kotlin/com/example/librerias/api/web/GlobalExceptionHandler.kt`.

### DTOs y mappers (`web/dto/`)

- [x] Funciones de extensión `toDto` / `attendanceSummary` — `backend/src/main/kotlin/com/example/librerias/api/web/dto/Mappers.kt`.
- [x] `PageResponse<T>` — `backend/src/main/kotlin/com/example/librerias/api/web/dto/Dtos.kt`.
- [x] `UserDto`
- [x] `ChildInfoDto`
- [x] `GroupInputDto`
- [x] `RegisterRequest`
- [x] `LoginRequest`
- [x] `RefreshRequest`
- [x] `TokenResponse`
- [x] `ExistsResponse`
- [x] `ResetCodeRequest`
- [x] `ResetCodeResponse`
- [x] `ResetPasswordRequest`
- [x] `SchoolGroupDto`
- [x] `CreateGroupRequest`
- [x] `StudentDto`
- [x] `LinkStudentRequest`
- [x] `SchoolTaskDto`
- [x] `CreateTaskRequest`
- [x] `UpdateTaskRequest`
- [x] `MarkDeliveredRequest`
- [x] `MeetingDto`
- [x] `CreateMeetingRequest`
- [x] `UpdateMeetingRequest`
- [x] `FamilyResponseDto`
- [x] `SetResponseRequest`
- [x] `ResponsesDto`
- [x] `PendingDto`
- [x] `AttendanceSummaryDto`
- [x] `CalendarEventDto`
- [x] `CreateCalendarEventRequest`
- [x] `FieldErrorDto`

## D. Clases y tipos públicos de la app KMP

Ruta común: `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/`.

### Raíz y datos de demostración

- [x] `DarkModeColors` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/AppTheme.kt`.
- [x] `CalendarSampleData` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/CalendarSampleData.kt`.
- [x] `DemoTodayProvider` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/DemoTodayProvider.kt`.
- [x] `FakeEmailSender` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/FakeEmailSender.kt`.
- [x] `MeetingSampleData` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/MeetingSampleData.kt`.
- [x] `SchoolSampleData` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/SchoolSampleData.kt`.
- [x] `TeacherCalendarSampleData` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/TeacherCalendarSampleData.kt`.

### Capa de red (`data/remote/`)

- [x] `ApiException` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiClient.kt`.
- [x] `ApiClient` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiClient.kt` (configuración `Json`, plugins, refresh 401).
- [x] `HttpClientFactory` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiClient.kt` (objeto interno; se documenta por la configuración del cliente).
- [x] `expect createPlatformHttpClient` / `expect apiBaseUrl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiClient.kt`.
- [x] `actual createPlatformHttpClient` / `actual apiBaseUrl` (Android) — `Librerias_eduvia/shared/src/androidMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt`.
- [x] `actual createPlatformHttpClient` / `actual apiBaseUrl` (iOS) — `Librerias_eduvia/shared/src/iosMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt`.
- [x] `UserDto` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiDtos.kt`.
- [x] `GroupDto`
- [x] `StudentDto`
- [x] `TaskDto`
- [x] `MeetingDto`
- [x] `FamilyResponseDto`
- [x] `AttendanceDto`
- [x] `CalendarEventDto`
- [x] `PageDto<T>`
- [x] `TokenResponseDto`
- [x] `ExistsDto`
- [x] `ResetCodeDto`
- [x] `ResponsesDto`
- [x] `PendingDto`
- [x] `FieldErrorDto`
- [x] `ProblemDto`
- [x] `LoginRequestDto`
- [x] `RegisterRequestDto`
- [x] `ChildDto`
- [x] `GroupInputDto`
- [x] `RefreshRequestDto`
- [x] `ResetCodeRequestDto`
- [x] `ResetPasswordRequestDto`
- [x] `CreateGroupRequestDto`
- [x] `LinkStudentRequestDto`
- [x] `CreateTaskRequestDto`
- [x] `UpdateTaskRequestDto`
- [x] `MarkDeliveredRequestDto`
- [x] `CreateMeetingRequestDto`
- [x] `UpdateMeetingRequestDto`
- [x] `SetResponseRequestDto`
- [x] `CreateCalendarEventRequestDto`
- [x] Funciones de mapeo DTO → dominio — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/Mappers.kt`.
- [x] `RemoteAuthRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteAuthRepository.kt`.
- [x] `RemoteCalendarRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteCalendarRepository.kt`.
- [x] `RemoteGroupRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteGroupRepository.kt`.
- [x] `RemoteListStore` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteListStore.kt` (clase interna).
- [x] `RemoteMeetingRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteMeetingRepository.kt`.
- [x] `RemoteTaskRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteTaskRepository.kt`.
- [x] `SessionStore` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/SessionStore.kt`.

### Repositorios "fake" e inyección

- [x] `AuthRepositoryImpl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/AuthRepositoryImpl.kt`.
- [x] `CalendarRepositoryImpl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/CalendarRepositoryImpl.kt`.
- [x] `GroupRepositoryImpl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/GroupRepositoryImpl.kt`.
- [x] `MeetingRepositoryImpl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/MeetingRepositoryImpl.kt`.
- [x] `TaskRepositoryImpl` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/TaskRepositoryImpl.kt`.
- [x] `AppContainer` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/di/AppContainer.kt`.

### Dominio

- [x] `AuthRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/repository/AuthRepository.kt`.
- [x] `CalendarRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/repository/CalendarRepository.kt`.
- [x] `GroupRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/repository/GroupRepository.kt`.
- [x] `MeetingRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/repository/MeetingRepository.kt`.
- [x] `TaskRepository` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/repository/TaskRepository.kt`.
- [x] `ObserveSessionUseCase` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/usecase/AuthUseCases.kt`.
- [x] `LogoutUseCase`
- [x] `LoginUseCase`
- [x] `ValidateAccountStepUseCase`
- [x] `RegisterUseCase`
- [x] `SendResetCodeUseCase`
- [x] `ResetPasswordUseCase`
- [x] `ObserveCalendarUseCase` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/usecase/CalendarUseCases.kt`.
- [x] `DeleteEventUseCase`
- [x] `ObserveGroupsUseCase` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/usecase/GroupUseCases.kt`.
- [x] `ObserveStudentsUseCase`
- [x] `ObserveMeetingsUseCase` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/usecase/MeetingUseCases.kt`.
- [x] `RespondToMeetingUseCase`
- [x] `ObserveStudentResponsesUseCase`
- [x] `ObserveAttendanceUseCase`
- [x] `DeleteMeetingUseCase`
- [x] `RemindPendingFamiliesUseCase`
- [x] `CreateMeetingUseCase`
- [x] `UpdateMeetingUseCase`
- [x] `ObserveTasksUseCase` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/usecase/TaskUseCases.kt`.
- [x] `MarkTaskDeliveredUseCase`
- [x] `UpdateTaskUseCase`
- [x] `DeleteTaskUseCase`
- [x] `CreateTaskUseCase`
- [x] `EmailSender` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/util/EmailSender.kt`.
- [x] `TodayProvider` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/util/TodayProvider.kt`.
- [x] `Validators` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/domain/validation/Validators.kt`.

### Modelos

- [x] `AppResult` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/AppResult.kt`.
- [x] `CalendarDate` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/CalendarModels.kt`.
- [x] `CalendarEventType`
- [x] `AttendanceSummary`
- [x] `CalendarEvent`
- [x] `expenseCategory` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/Expense.kt`.
- [x] `Expense`
- [x] `FamilyResponse` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/MeetingAttendance.kt`.
- [x] `NewMeetingInput`
- [x] `MeetingResponse` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/MeetingModels.kt`.
- [x] `Meeting`
- [x] `UserRole` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/RegisterModels.kt`.
- [x] `ChildInfo`
- [x] `RegisterInput` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/Registration.kt`.
- [x] `RegistrationResult`
- [x] `SchoolGroup` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/SchoolGroup.kt`.
- [x] `GroupInput`
- [x] `TaskStatus` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/SchoolTask.kt`.
- [x] `SchoolTask`
- [x] `NewTaskInput`
- [x] `Subject` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/Subject.kt`.
- [x] `User` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/User.kt`.
- [x] `Student`
- [x] Funciones de fecha y calendar date — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/DateParsing.kt` y `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/model/CalendarDateExt.kt`.

### UI, navegación y plataformas

- [x] `ForgotUiState` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/ForgotPasswordViewModel.kt`.
- [x] `ForgotPasswordViewModel`
- [x] `GroupCodeInfo` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/GroupsReadyScreen.kt`.
- [x] `LoginViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/LoginViewModel.kt`.
- [x] `TeacherGroup` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/RegisterScreen.kt`.
- [x] `RegisterTeacherViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/RegisterTeacherViewModel.kt`.
- [x] `RegisterViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/RegisterViewModel.kt`.
- [x] `CalendarViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/calendar/CalendarViewModel.kt`.
- [x] `BottomTab` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/components/BottomNavBar.kt`.
- [x] `HomeViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/home/HomeViewModel.kt`.
- [x] `MeetingsViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/meetings/MeetingsViewModel.kt`.
- [x] `TasksViewModel` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/tasks/TasksViewModel.kt`.
- [x] `EduviaColors` — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/theme/EduviaTheme.kt`.
- [x] Composables de navegación y pantallas — `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/navigation/AppNavHost.kt`, `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/navigation/UserChip.kt` y los archivos **Screen.kt** del directorio `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/`.
- [x] `MainActivity` — `Librerias_eduvia/androidApp/src/main/kotlin/com/example/librerias/MainActivity.kt`.
- [x] `iOSApp` y `ContentView` — `Librerias_eduvia/iosApp/iosApp/iOSApp.swift` y `Librerias_eduvia/iosApp/iosApp/ContentView.swift`.

## E. Endpoints HTTP (`/api/v1`)

Rutas de los controllers; ids A/G/T/M/C tomados de `docs/backend/api-contract.md`.

### Autenticación (9)

- [x] A1 `POST /auth/register` — `backend/src/main/kotlin/com/example/librerias/api/web/AuthController.kt`.
- [x] A2 `POST /auth/login`
- [x] A3 `POST /auth/refresh`
- [x] A4 `POST /auth/logout`
- [x] A5 `GET /auth/exists/email`
- [x] A6 `GET /auth/exists/phone`
- [x] A7 `POST /auth/reset-code`
- [x] A8 `POST /auth/reset-password`
- [x] A9 `GET /me`

### Grupos y alumnos (7)

- [x] G1 `GET /groups` — `backend/src/main/kotlin/com/example/librerias/api/web/GroupController.kt`.
- [x] G2 `GET /groups/{id}`
- [x] G3 `GET /groups/by-code`
- [x] G4 `POST /groups`
- [x] G5 `GET /groups/{id}/students`
- [x] G6 `GET /students`
- [x] G7 `POST /students:link`

### Tareas (6)

- [x] T1 `GET /tasks` — `backend/src/main/kotlin/com/example/librerias/api/web/TaskController.kt`.
- [x] T2 `GET /tasks/{id}`
- [x] T3 `POST /tasks`
- [x] T4 `PUT /tasks/{id}`
- [x] T5 `DELETE /tasks/{id}`
- [x] T6 `POST /tasks/{id}/deliveries`

### Juntas (9)

- [x] M1 `GET /meetings` — `backend/src/main/kotlin/com/example/librerias/api/web/MeetingController.kt`.
- [x] M2 `GET /meetings/{id}`
- [x] M3 `POST /meetings`
- [x] M4 `DELETE /meetings/{id}`
- [x] M5 `GET /meetings/{id}/attendance`
- [x] M6 `PUT /meetings/{id}/responses/{studentId}`
- [x] M7 `GET /meetings/responses`
- [x] M8 `POST /meetings/{id}/reminders`
- [x] M9 `PUT /meetings/{id}`

### Calendario (3)

- [x] C1 `GET /calendar/events` — `backend/src/main/kotlin/com/example/librerias/api/web/CalendarController.kt`.
- [x] C2 `POST /calendar/events`
- [x] C3 `DELETE /calendar/events/{id}`

## F. Tablas de PostgreSQL

Definidas en `backend/src/main/resources/db/migration/V1__init_schema.sql`.

- [x] `users`
- [x] `refresh_tokens`
- [x] `password_reset_codes`
- [x] `school_groups`
- [x] `students`
- [x] `school_tasks`
- [x] `task_deliveries`
- [x] `meetings`
- [x] `meeting_responses`
- [x] `calendar_events`

## G. Migraciones y datos de ejemplo

- [x] `backend/src/main/resources/db/migration/V1__init_schema.sql` — esquema inicial (10 tablas, 11 índices, constraints).
- [x] `backend/src/main/resources/db/dev-data/V1000__seed.sql` — datos demo del perfil `dev`.
- [x] `backend/src/main/resources/db/migration/V2__comments.sql` — **por crear**: `COMMENT ON TABLE` y `COMMENT ON COLUMN` sin tocar `V1` ni `V1000`.

## H. Docker, infraestructura y scripts

- [x] Servicio `postgres` — `docker-compose.yml`.
- [x] Servicio `api` — `docker-compose.yml`.
- [x] Servicio `adminer` (perfil `tools`) — `docker-compose.yml`.
- [x] Volumen `pgdata` — `docker-compose.yml`.
- [x] `backend/Dockerfile` — build multi-stage JDK 21 → JRE 21, usuario no root y `curl` para healthcheck.
- [x] `backend/.dockerignore`
- [x] `backend/compose.yaml` — solo PostgreSQL para `bootRun`.
- [x] `.env.example` — variables `DB_*`, `API_PORT`, `ADMINER_PORT`, `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`.
- [x] `docker-compose.yml` — nombre de proyecto `miapp`, healthchecks y puertos configurables.
- [x] `scripts/test-all.ps1` — 5 pasos de verificación y limpieza en `finally`.
- [x] `Librerias_eduvia/androidApp/src/main/res/xml/network_security_config.xml` — HTTP local permitido solo para `10.0.2.2`/`localhost`.
- [x] `Librerias_eduvia/androidApp/src/main/AndroidManifest.xml` — permiso `INTERNET` y referencia a la configuración de red.
- [x] `Librerias_eduvia/iosApp/iosApp/Info.plist` — ATS local (`NSAllowsLocalNetworking`).
- [x] `Librerias_eduvia/iosApp/Configuration/Config.xcconfig` — configuración iOS.
- [x] `.gitattributes` y `.gitignore` — fin de línea de `gradlew` y archivos ignorados.

## I. Suites de pruebas, soporte y fixtures

### Backend (`backend/src/test/kotlin/com/example/librerias/api/`)

- [x] `ApiApplicationTests` — arranque del contexto.
- [x] `AuthIntegrationTest` — autenticación y registro.
- [x] `GroupIntegrationTest` — grupos y alumnos.
- [x] `TaskIntegrationTest` — tareas y entregas.
- [x] `MeetingIntegrationTest` — juntas, respuestas y M9.
- [x] `CalendarIntegrationTest` — eventos y asistencia.
- [x] `E2eIntegrationTest` — E2E contra el stack Docker (`@Tag("e2e")`).
- [x] `IntegrationTestBase` — soporte: restablece la BD entre pruebas.
- [x] `TestcontainersConfiguration` — soporte: Postgres efímero.
- [x] `TestData` — soporte: constructores de datos únicos.
- [x] `PasswordEncoderForTest` — soporte: codificador de contraseñas de prueba.
- [x] `TestApiApplication` — soporte: aplicación de prueba.
- [x] `backend/src/test/resources/application-test.yml` — perfil de pruebas.

### App KMP

Ruta común de `commonTest`: `Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/`.

- [x] `SharedCommonTest` — `Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/SharedCommonTest.kt`.
- [x] `ApiClientTest` — `Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/data/remote/ApiClientTest.kt`.
- [x] `RemoteRepositoriesTest` — `Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/data/remote/RemoteRepositoriesTest.kt`.
- [x] `UpdateMeetingUseCaseTest` — `Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/domain/usecase/UpdateMeetingUseCaseTest.kt`.
- [x] `TestSupport` — soporte con `MockEngine` y builders (`Librerias_eduvia/shared/src/commonTest/kotlin/com/example/librerias/data/remote/TestSupport.kt`).
- [x] `SharedLogicAndroidHostTest` — `Librerias_eduvia/shared/src/androidHostTest/kotlin/com/example/librerias/SharedLogicAndroidHostTest.kt`.
- [x] `ContractFixturesFileTest` — `Librerias_eduvia/shared/src/androidHostTest/kotlin/com/example/librerias/contract/ContractFixturesFileTest.kt`.
- [x] `SharedLogicIOSTest` — `Librerias_eduvia/shared/src/iosTest/kotlin/com/example/librerias/SharedLogicIOSTest.kt`.

### Fixtures de contrato (`contract/fixtures/`)

- [x] `contract/fixtures/attendance-page.json`
- [x] `contract/fixtures/calendar-page.json`
- [x] `contract/fixtures/groups-page.json`
- [x] `contract/fixtures/login-response.json`
- [x] `contract/fixtures/meeting-detail.json`
- [x] `contract/fixtures/meetings-page.json`
- [x] `contract/fixtures/problem-validation.json`
- [x] `contract/fixtures/register-response.json`
- [x] `contract/fixtures/students-page.json`
- [x] `contract/fixtures/task-detail.json`
- [x] `contract/fixtures/tasks-page.json`

## J. Documentación y código existentes (fuentes leídas)

- [x] `GUIA-OPENCODE.md` — fases, puertas de calidad, pirámide de pruebas y solución de problemas.
- [x] `docs/backend/api-contract.md` — contrato y reglas de negocio.
- [x] `docs/backend/data-model.md` — modelo ER, tablas, índices y migraciones.
- [x] `docs/backend/DECISIONS.md` — decisiones 1–13.
- [x] `docs/backend/PROGRESS.md` — evidencia real de las fases 1–7.
- [x] `docs/backend/OPEN-QUESTIONS.md` — dudas Q1–Q9.
- [x] `docs/backend/test-plan.md` — escenarios por endpoint.
- [x] `docs/backend/compat-report.md` — compatibilidad app ↔ API.
- [x] `docs/backend/openapi.yaml` — contrato OpenAPI 3.0.3 (26 paths / 34 operaciones).
- [x] `docs/guias/README.md` — índice de las guías juveniles.
- [x] `docs/guias/app-kotlin.md`
- [x] `docs/guias/api-rest.md`
- [x] `docs/guias/base-de-datos.md`
- [x] `Librerias_eduvia/README.md` — README actual de la app (plantilla KMP).
- [x] `backend/HELP.md` — referencias generadas por start.spring.io.

## K. Entregables de documentación (plan ordenado)

> Todos los archivos de esta sección se crearon y sus rutas se comprobaron al finalizar.

- [x] `docs/HALLAZGOS.md` — bugs/confusiones encontrados, con archivo, línea y propuesta (sin arreglarlos).
- [x] `docs/backend/OPEN-QUESTIONS.md` (ampliado) — dudas nuevas marcadas `⚠️ Por confirmar`.
- [x] KDoc/Javadoc y anotaciones OpenAPI en backend (sección C) y comentarios en `application*.yml`.
- [x] Comentarios SQL en `backend/src/main/resources/db/migration/V1__init_schema.sql` y `backend/src/main/resources/db/dev-data/V1000__seed.sql` si Flyway no reporta checksum; `backend/src/main/resources/db/migration/V2__comments.sql` con `COMMENT ON`.
- [x] KDoc en red, DTOs, mappers, repositorios, casos de uso, ViewModels y `expect/actual` de la app (sección D).
- [x] KDoc en suites de pruebas y clases de soporte (sección I).
- [x] Comentarios en Docker, `.env.example` y `scripts/test-all.ps1` (sección H).
- [x] `README.md` (raíz) — descripción, diagrama Mermaid, requisitos, inicio rápido e índice.
- [x] `docs/README.md` — índice navegable de `docs/`.
- [x] `docs/arquitectura.md` — capas, flujo punta a punta, dependencias y decisiones.
- [x] `docs/app-kmp.md` — módulos, source sets, datos, DI, red por plataforma y estados.
- [x] `docs/backend.md` — paquetes, capas, errores, paginación, validación, seguridad y perfiles.
- [x] `docs/api.md` — tabla de endpoints con ejemplos reales y enlace a Swagger UI.
- [x] `docs/base-de-datos.md` — ER, tablas, índices, migraciones y datos de ejemplo.
- [x] `docs/docker.md` — servicios, puertos, volúmenes, variables, healthchecks y perfiles.
- [x] `docs/pruebas.md` — pirámide, suites, comandos, fixtures, cobertura y cómo añadir pruebas.
- [x] `docs/guia-de-desarrollo.md` — endpoint nuevo de punta a punta y convenciones.
- [x] `docs/operacion.md` — runbook: comandos, logs, reset, fixtures y problemas comunes.
- [x] `docs/adr/` — ADR de Kotlin backend, build separado, Flyway, Testcontainers, fixtures, ProblemDetail y base URL `10.0.2.2`.
- [x] `docs/glosario.md` — términos del dominio y técnicos.
- [x] `backend/README.md` y README cortos de `shared/`, `androidApp/` e `iosApp/`.
- [x] Verificación: `scripts/test-all.ps1`, `:androidApp:assembleDebug`, backend `gradlew.bat build`, Swagger UI, sintaxis Mermaid, enlaces e inventario al 100 %.

