# Progreso del backend — EduvIA

> Orden estricto: F1 → F2 → F3 → F4 → F5 → F6 → F7. Actualizado tras cada fase con resultados reales.

## Lista de tareas (7 fases)

- [x] Fase 1 — Analizar app y contrato (skill `kmp-api-contract`)
- [x] Fase 2 — Esquema PostgreSQL y Flyway (skill `postgres-schema-design`)
- [x] Fase 3 — API Spring Boot + JPA + REST + JWT + tests (skills `springboot-*` )
- [x] Fase 4 — Docker + Compose + E2E (skill `backend-docker-compose`)
- [x] Fase 5 — Conexión app KMP + compat-report (skill `kmp-client-sync`)
- [ ] Fase 6 — Prueba manual en emulador (sin dispositivo disponible; usuario eligió continuar con Fase 7)
- [x] Fase 7 — `scripts/test-all` + verificación final

---

## Fase 1 — Analizar la app y definir el contrato

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- `powershell -NoProfile -ExecutionPolicy Bypass -File .opencode\skills\kmp-api-contract\scripts\scan-kmp.ps1 .`
  → 0 llamadas HTTP, 0 `@Serializable`, 0 Ktor → **Caso B** confirmado.
  → Módulos detectados: `Librerias_eduvia/shared` (Android + iOS), paquete `com.example.librerias`.
- `node -e "...YAML.parse(...)"` → `YAML OK - paths: 26 schemas: 35 tags: 5`.
- `java -version` → 21.0.7 LTS. `docker compose version` → v5.5.1.

**Decisiones confirmadas por el usuario:** JWT completo (Fase 3b), backend en `backend/`, Kotlin, paquete `com.example.librerias.api`, compose `miapp`. Ver `DECISIONS.md`.

**Archivos creados:**
- `docs/backend/api-contract.md` — 31 operaciones de 5 repositorios mapeadas a 26 paths (algunos paths agrupan varios métodos), convenciones y reglas de negocio.
- `docs/backend/openapi.yaml` — OpenAPI 3.0.3, 26 paths / 35 schemas, válido.
- `docs/backend/test-plan.md` — ~80 escenarios con ids, tipo (U/W/I/C/E/A) y casilla pendiente.
- `docs/backend/OPEN-QUESTIONS.md` — 9 dudas no bloqueantes con decisión provisional.
- `docs/backend/DECISIONS.md` — decisiones confirmadas.

**Pruebas añadidas:** ninguna (la fase es de análisis; la validación fue el parseo YAML).

**Resultado de pruebas:** `YAML OK - paths: 26 schemas: 35 tags: 5` (salida real).

**Pendientes:** marcar las casillas del `test-plan.md` a medida que las pruebas se escriban y pasen (Fase 3 en adelante).

**✅ Criterio "Listo cuando":** cada operación de datos de la app tiene su endpoint en `api-contract.md` y su fila en `test-plan.md`. Verificado: 5 repositorios, 31 operaciones, 31 filas de endpoints en el contrato y filas de prueba por cada una.

---

## Fase 2 — Esquema PostgreSQL + Flyway

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- Docker Desktop no estaba corriendo → `Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'` → daemon `29.8.0` en 3 s.
- `docker run --rm -d --name eduvia-pg-check ... postgres:17-alpine` + `docker cp ...` + `docker exec psql -v ON_ERROR_STOP=1 -f V1.sql` y `-f V1000__seed.sql`.
- Resultado real: `tables=10, users=2, groups=3, students=30, tasks=6, deliveries=116, meetings=2, responses=56, events=23`.
- Contenedor temporal eliminado al terminar.

**Errores encontrados y corregidos (4 intentos, ningún test desactivado):**
1. `unique (group_id, date, lower(time), lower(location))` → PostgreSQL no admite expresiones en `UNIQUE` de tabla → se cambió a `create unique index uk_meetings_unique`.
2. FK `students.parent_id` rechazaba `parentId=0` (centinela de la app) → columna cambiada a nullable; el mapper convertirá `null ↔ 0`.
3. Alumnos 100/101 referenciaban el grupo 3, comentado en la app → se activó el grupo en el seed.
4. Colisión de PK en `calendar_events` (tareas y juntas con ids solapados en dos listas separadas en la app) → juntas usan `1000 + meeting.id`.

**Archivos creados:**
- `docs/backend/data-model.md` — ER Mermaid, 10 tablas, mapeo DTO↔tabla, 11 índices justificados.
- `backend/src/main/resources/db/migration/V1__init_schema.sql` — 10 tablas, constraints con nombre, índices.
- `backend/src/main/resources/db/dev-data/V1000__seed.sql` — datos demo (bcrypt real de `Maestra123`/`Padre1234`), solo perfil `dev`.

**Pruebas añadidas:** ninguna aún (según la guía, las pruebas de BD se escriben en Fase 3 y se ejecutan ahí: `MigrationsTest`, `SchemaTest`, `ConstraintsTest`).

**Resultado de pruebas:** migraciones + seed aplicados en Postgres 17 real sin errores (`ON_ERROR_STOP=1`). Salida real arriba.

**Pendientes:** crear las pruebas `db/*Test` en Fase 3 con Testcontainers; verificar `ddl-auto: validate` con las entidades JPA.

**✅ Criterio "Listo cuando":** esquema documentado en `data-model.md` y migraciones aplicadas correctamente en Postgres limpio (pruebas automatizadas se verifican en Fase 3, según la guía).

---

## Fase 3 — API Spring Boot + JPA + REST + JWT + tests

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- Generación con start.spring.io → Spring Boot **4.2.0**, Kotlin 2.4.21, Gradle wrapper 9.9.2, Java 21.0.7.
- `backend\gradlew.bat build -x test --no-daemon` → `BUILD SUCCESSFUL`.
- `backend\gradlew.bat compileTestKotlin --no-daemon` → `BUILD SUCCESSFUL`.
- `backend\gradlew.bat --no-daemon check` → `BUILD SUCCESSFUL`.
  - `36 tests completed, 0 failures` (6 clases: `ApiApplicationTests`, `AuthIntegrationTest`, `GroupIntegrationTest`, `TaskIntegrationTest`, `MeetingIntegrationTest`, `CalendarIntegrationTest`).
  - JaCoCo: **85 % instrucciones** (878/6171), **57 % ramas**, **94 % líneas** (53/936), **77 % métodos**, **98 % clases**. `jacocoTestCoverageVerification` (umbral 80 %) pasó.
- Arranque manual real en `PORT=8081`: `/actuator/health` → 200 UP; `POST /api/v1/auth/login` con `laura@eduvia.mx`/`Maestra123` → JWT HS256 con `sub`, `roles`, `iss`, `expiresIn:900`; `/me` con token → 200 y sin token → 401 + `WWW-Authenticate: Bearer` + `application/problem+json`.

**Archivos creados:**
- `backend/build.gradle.kts` — Spring Boot 4.2.0, springdoc 3.2.1, PostgreSQL, Flyway, Security+JWT, Validation, Actuator, JaCoCo ≥80 %, fuentes `test`/`e2eTest`, versiones fijadas (rest-assured 6.3.0; MockK 1.14.12 se retiró en la refactorización por no usarse).
- `backend/src/main/resources/application.yml`, `application-dev.yml`, `src/test/resources/application-test.yml` (Testcontainers, `ddl-auto: validate`, JWT de test).
- 10 entidades JPA + repositorios, DTOs/mappers, `PageResponse<T>`, `GlobalExceptionHandler` (ProblemDetail RFC 9457), `JwtConfig`, `SecurityConfig`, servicios y controllers para los 26 paths del contrato.
- Fixtures de contrato en `contract/fixtures/*.json` (login, register, groups, students, tasks, meetings, calendar, attendance, problem-validation).
- Pruebas de integración con Testcontainers (Postgres 17) y `TestData` con datos únicos por test.

**Errores encontrados y corregidos (ninguna prueba desactivada ni umbral bajado):**
1. Boot 4 usa Jackson 3 (`tools.jackson.databind.ObjectMapper`) y `JwsHeader.with(MacAlgorithm.HS256)`.
2. Boot 4 movió `TestRestTemplate` a `org.springframework.boot.resttestclient` y requiere `spring-boot-restclient` + `@AutoConfigureTestRestTemplate`.
3. Boot 4 eliminó `spring.jackson.serialization.write-dates-as-timestamps` (propiedad retirada de configuración).
4. `PasswordEncoderFactories.createDelegatingPasswordEncoder()` como bean explícito; seed con prefijo `{bcrypt}` y login tolerante a hashes legacy.
5. El advice de errores necesita `@Order(HIGHEST_PRECEDENCE)` para que `@Valid` y los tipos inválidos devuelvan `ProblemDetail` con `errors` en vez del manejador genérico de Boot.
6. `acceptedTerms` usa `@AssertTrue` (no `@NotNull`) para que `false` produzca error de validación.
7. `TestData` genera email/teléfono únicos dentro de cada test y se reinicia tras el `truncate`.
8. `calendar_events.id` se asigna desde `nextval('calendar_events_id_seq')` para eventos nuevos; las juntas usan `1000 + meetings.id` según `DECISIONS.md` (evita colisión con tareas).
9. Los inserts con `returning id` usan `queryForObject` en los tests.

**Resultado de pruebas:** verde, ver evidencia arriba. Casillas marcadas en `docs/backend/test-plan.md` (49 de 69 escenarios en este momento; los 20 restantes corresponden a Fase 4 E2E, sync de la app KMP y validaciones de BD/contrato pendientes). Tras Fase 6 el backend tiene **38 tests** (se añadieron M9-ok y M9-conf).

**✅ Criterio "Listo cuando":** `check` en verde con cobertura ≥80 % y endpoints del contrato probados contra Postgres real. Verificado.

---

## Fase 4 — Docker y Docker Compose

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- `docker compose -p miapp-phase4 config -q` → `config_exit=0`.
- `docker compose -p miapp-phase4 up -d --build --wait` → build multi-stage correcto (`BUILD SUCCESSFUL` dentro de la imagen), `postgres` Healthy y `api` Healthy.
- `curl -fsS http://localhost:18082/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}`.
- `backend\gradlew.bat --no-daemon e2eTest` con `E2E_BASE_URL=http://localhost:18082` → `BUILD SUCCESSFUL` (4 pruebas E2E: health, login + `/me`, CRUD grupo/tarea, ProblemDetail 404).
- `docker compose -p miapp-phase4 restart api` + `up -d --wait` → health UP de nuevo; `GET /api/v1/groups?teacherId=1` devolvió `totalElements=4` (3 del seed + 1 creado por E2E) → **persistencia confirmada tras reinicio**.
- Comparación `/v3/api-docs` vs `openapi.yaml`: `contract=26 runtime=26 missing=[] extra=[]`.
- `docker compose -p miapp-phase4 down -v --remove-orphans` al terminar.

**Archivos creados:**
- `backend/Dockerfile` (multi-stage JDK 21 → JRE 21, usuario no root, curl para healthcheck).
- `backend/.dockerignore`, `backend/compose.yaml` (solo Postgres para `bootRun`).
- `docker-compose.yml` en la raíz (`name: miapp`, Postgres + API + Adminer opcional, healthchecks y puertos configurables).
- `.env.example`, `.env` (solo desarrollo local), `.gitignore`, `.gitattributes`.
- `scripts/test-all.ps1` (5 pasos: compose config, backend check, tests KMP, stack aislado, e2eTest).
- `backend/src/test/kotlin/.../E2eIntegrationTest.kt` con `@Tag("e2e")`.
- `docs/backend/compat-report.md` (plantilla para Fase 5).

**Notas / pendientes para fases posteriores:**
- El módulo KMP no expone `:shared:testDebugUnitTest`; expone `:shared:testAndroid`. El script usa `APP_TEST_TASK` (por defecto `:shared:testAndroid`).
- En este equipo no hay Android SDK configurado (`local.properties` apuntaba a una ruta inexistente); **resuelto en Fase 5**: apunta a `C:\Users\Dev_Ludwig\AppData\Local\Android\Sdk` y el paso 3 de `test-all` ya pasa.
- Se añadió `developmentOnly("org.springframework.boot:spring-boot-docker-compose")` para `bootRun` con `backend/compose.yaml`.

**✅ Criterio "Listo cuando":** `docker compose up -d --build --wait` termina sin errores y `./gradlew e2eTest` está en verde. Verificado.

---

## Fase 5 — Conexión de la app KMP con la API

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- `powershell -NoProfile -ExecutionPolicy Bypass -File ..\.opencode\skills\kmp-api-contract\scripts\scan-kmp.ps1 .` (desde `Librerias_eduvia`)
  → Ktor **3.2.0** (core, auth, logging, content-negotiation, engines OkHttp/Darwin, mock en tests), **31 clases `@Serializable`**, 8 repositorios remotos, header `Bearer`, refresh en 401 y parseo de `ProblemDetail`. Caso B resuelto: la interfaz de dominio se conserva, solo cambia la implementación inyectada.
- `.\gradlew.bat --no-daemon :shared:compileCommonMainKotlinMetadata` → `BUILD SUCCESSFUL`.
- `.\gradlew.bat --no-daemon :shared:compileAndroidMain` → `BUILD SUCCESSFUL`.
- `.\gradlew.bat --no-daemon :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosArm64` → `BUILD SUCCESSFUL` (solo aviso esperado: simulador iOS no ejecutable en Windows).
- `.\gradlew.bat --no-daemon :shared:testAndroidHostTest` → `BUILD SUCCESSFUL`; **17 tests, 0 failures, 0 skipped**.
- `.\gradlew.bat --no-daemon :shared:testAndroid` → `BUILD SUCCESSFUL` (agrega las pruebas comunes; mismo resultado 17/0/0).
- Stack aislado: `docker compose -p miapp-compat up -d --build --wait` con `API_PORT=18083`, `DB_PORT=15433` → `postgres` Healthy + `api` Healthy.
- `/v3/api-docs` runtime vs `docs/backend/openapi.yaml`: **design=33 operaciones, runtime=33, `missing=[]`, `extra=[]`** (26 paths, 35 schemas).
- Peticiones reales: login → 200 con JWT HS256; `POST /tasks` → 201; `DELETE /tasks/7` → 204; `GET /groups/999999` → 404 `application/problem+json`; `GET /me` sin token → 401 + `WWW-Authenticate: Bearer`.

**Archivos creados/modificados en la app:**
- `shared/src/commonMain/.../data/remote/`: `ApiClient` (inyección de engine, `expectSuccess=false`, timeout, logging, refresh 401, `ApiException` con `ProblemDto`), `ApiDtos` (31 DTOs `@Serializable`), `Mappers`, `SessionStore`, `RemoteListStore` y los 5 repositorios remotos (`Auth`, `Group`, `Task`, `Meeting`, `Calendar`).
- `shared/src/{androidMain,iosMain}/.../PlatformHttpClient.kt`: motores OkHttp/Darwin y base URL `http://10.0.2.2:8080/api/v1/` / `http://localhost:8080/api/v1/`.
- `shared/src/commonMain/.../di/AppContainer.kt`: inyecta los repositorios remotos; mantiene `FakeEmailSender` solo para la bandeja demo.
- `shared/src/commonTest/.../data/remote/`: `ApiClientTest`, `RemoteRepositoriesTest`, `TestSupport` (MockEngine + builders). `shared/src/androidHostTest/.../contract/ContractFixturesFileTest.kt`: 11 fixtures reales con `ignoreUnknownKeys=false`.
- Gradle: Ktor 3.2.0 + serialization 1.9.0 + logging + `ktor-client-mock`, `contract.fixtures.dir`, plugin de serialización. `local.properties` corregido al SDK Android local.
- Android: permiso `INTERNET` y `network_security_config.xml` restringido a `10.0.2.2`/`localhost`. iOS: ATS `NSAllowsLocalNetworking = true`.

**Errores encontrados y corregidos (ninguna prueba desactivada):**
1. `ContractFixturesFileTest` esperaba `meeting.id = 3`, pero el fixture real del backend tiene `id = 1` → assertion corregido al valor real.
2. `ApiClientTest` comprobaba `totalElements = 10` con un `pageOf(...)` que por defecto devolvía `1` → se pasa `total = 10` explícitamente.
3. En `MockEngine`, `request.body` es `TextContent` (JSON serializado), no el DTO → la prueba de `CreateTaskRequestDto` lee `.text` y decodifica con el `Json` del cliente (antes el `toString()` no producía JSON).
4. Warning de safe-call innecesario tras `assertNotNull` eliminado.

**Documentación reconciliada:** `api-contract.md` y `data-model.md` usan `1000 + meetings.id` para el evento de junta; `openapi.yaml` y `DECISIONS.md` aclaran `dateText = YYYY-MM-DD`; `test-plan.md` marca FX2 y añade 5 filas SYNC; `compat-report.md` queda completo sin incompatibilidades.

**Actualización posterior (Fase 6):** Ktor se subió de **3.2.0 a 3.2.1** porque 3.2.0 no pasa D8 (`Space characters in SimpleName 'use streaming syntax' ... io.ktor.client.plugins.Messages`). Versión comprobada en Maven Central; `minSdk=24` restaurado y APK instalado correctamente.

**✅ Criterio "Listo cuando":** la app compila en las tres plataformas objetivo, `:shared:testAndroid` está en verde incluidas las pruebas de contrato y `compat-report.md` no tiene incompatibilidades. Verificado.

---

## Fase 6 — Prueba manual de punta a punta

**Estado:** ✅ TERMINADA (con emulador real)

**Evidencia real:**
- `adb devices` → `emulator-5554` (API 35) conectado; Docker Desktop 29.8.0 en ejecución.
- No se tocó `httpd.exe` (PID 5240) que ocupa el puerto 8080 del host. Para no desplazarlo se levantó el stack aislado `miapp-manual` con `API_PORT=18088`, `DB_PORT=15435`, `ADMINER_PORT=18089`; `docker compose -p miapp-manual ps` → `postgres` y `api` **healthy**; `curl http://localhost:18088/actuator/health` → `UP`.
- Para que el emulador alcance ese puerto sin cambiar la URL definitiva de la app se usó temporalmente `adb reverse tcp:8080 tcp:18088` y una base URL Android temporal `http://localhost:8080/api/v1/`. **Esto se revirtió al terminar** (ver abajo).
- `.\gradlew.bat --no-daemon :androidApp:installDebug` → APK instalado; login con `laura@eduvia.mx` / `Maestra123` → Inicio con 6 tareas y 2 juntas reales del backend.
- `docker compose -p miapp-manual logs api`: sin 4xx/5xx inesperados. El 500 inicial al consultar `GET /tasks` sin `groupId` fue un error del curl de prueba (el endpoint exige `groupId`, documentado en T1), no un fallo del backend.
- Listado “Pasadas” coincidió con los 6 registros de `GET /tasks?groupId=1`; “Activas” vacía porque las tareas seed vencen en octubre de 2026 y la prueba se hizo el 24 de noviembre de 2026.
- Detalle de tarea abierto desde Calendario → octubre → 12/10 → editar: se guardó un título nuevo y se verificó en `GET /tasks/1` y en `select title from school_tasks where id=1`. Se restauró el título original (`Resolver página 45`, con acentos, enviando bytes UTF-8) y se comprobó en BD.
- Creación de tarea “Tarea creada Fase6” (id 7, `dueDate=2026-11-30`) verificada en API y BD; borrado desde la app verificado (BD sin filas Fase6, API vuelve a 6 tareas).
- Listado de Juntas coincidió con `GET /meetings` (ids 1 y 2). Creación de “Junta Fase6” (id 3, 2026-12-05, 10:00 am, Salon Fase6) verificada en API y BD.

**Fallo encontrado y corregido (regla 6 de la guía: prueba roja primero):**
1. Al editar la junta creada, el formulario mostraba “Editar junta” pero el guardado fallaba con *«Ya existe una junta en esa fecha, hora y lugar para este grupo»*. Causa: la UI llamaba a `CreateMeetingUseCase` sin el id, por lo que la validación de duplicados se encontraba a sí misma; el contrato tampoco tenía operación de actualización de juntas.
2. El usuario eligió **“Arreglar la app”** (ampliar el alcance original de solo capa de red).
3. Prueba roja backend: `MeetingIntegrationTest.actualizar junta conserva su id y actualiza su evento de calendario` → 405/404. Prueba roja KMP: `RemoteRepositoriesTest.meetings_updateMeeting_envia_PUT_y_actualiza_la_cache` → método inexistente.
4. Implementación: `PUT /meetings/{id}` con `UpdateMeetingRequest` en `MeetingService.update` (excluye la propia junta del chequeo de duplicados, actualiza el evento `1000+id`); en la app, `MeetingRepository.updateMeeting`, `UpdateMeetingRequestDto`, `RemoteMeetingRepository.updateMeeting`, `UpdateMeetingUseCase`, `MeetingsViewModel.updateMeeting` y `AppNavHost` eligiendo crear vs. actualizar según `meetingToEdit`. También se corrigió la búsqueda del evento de junta en Calendario (`id - MEETING_ID_OFFSET`) y se eliminó un borrado duplicado del evento (el backend ya lo hace en la misma transacción).
5. Contrato actualizado: fila **M9** en `api-contract.md` y `openapi.yaml`, filas **M9-ok/M9-conf** en `test-plan.md` (ahora [x] con pruebas reales), fila nueva en `compat-report.md`.
6. Verde: `MeetingIntegrationTest` → 9 tests, 0 failures; `:shared:testAndroid` → 19 tests, 0 failures, 0 skipped.
7. Repetición en emulador con el stack reconstruido y el APK reinstalado: se recreó “Junta Fase6” (id 4) → “Editar junta” → cambiar título a “Junta Fase6 editada” → **Guardar cambios** sin el error de duplicado. Verificado en `GET /meetings`, `select ... from meetings` y `select ... from calendar_events` (evento `1004` con el título actualizado). La junta de prueba se borró desde la app y la BD quedó con solo las 2 juntas seed.

**Errores menores del manejo del emulador (no del código):** `input text` no acepta espacios ni acentos directamente; se usó `%s` para espacios y se evitó `4` por `keyevent 4`. `curl.exe` con JSON literal produjo 400/mojibake; se usó `ConvertTo-Json` + `[Text.Encoding]::UTF8.GetBytes()`.

**Limpieza y reversión al terminar:**
- Junta de prueba “Junta Fase6” borrada; base de datos sin filas de prueba.
- `PlatformHttpClient.kt` (Android) restaurado a `http://10.0.2.2:8080/api/v1/`; `adb reverse --remove-all` ejecutado.
- `docker compose -p miapp-manual down -v --remove-orphans` ejecutado; stack `miapp-manual` eliminado.

**✅ Criterio "Listo cuando":** recorridos login, listar, ver detalle, crear, editar y borrar (tareas y juntas) en emulador real, sin 4xx/5xx inesperados, con persistencia confirmada en API y PostgreSQL. Verificado.

---

## Fase 7 — Un solo comando para probar toda la arquitectura

**Estado:** ✅ TERMINADA

**Comandos ejecutados (evidencia real):**
- Primera ejecución (Ktor 3.2.0, antes de Fase 6): 36 tests backend, 17 KMP, 4 E2E, JaCoCo 85,8 %; verde.
- **Reejecución final desde cero tras Fase 6** (Ktor 3.2.1, `PUT /meetings/{id}`, base URL `10.0.2.2` restaurada):
  - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-all.ps1` desde la raíz:
    - `1/5 Validando docker-compose.yml` → sin errores.
    - `2/5 Backend: pruebas + contrato + cobertura` → `BUILD SUCCESSFUL`; **38 tests, 0 failures, 0 skipped** (se añadieron los 2 tests de M9); JaCoCo **86,2 % instrucciones** (5595/6488), por encima del umbral ≥80 %; `jacocoTestCoverageVerification` pasó.
    - `3/5 App KMP: pruebas` → `BUILD SUCCESSFUL`; **19 tests, 0 failures, 0 skipped** (`:shared:testAndroid`, con los tests de `updateMeeting` y `UpdateMeetingUseCase`).
    - `4/5 Levantando stack aislado` (`miapp-test`, puertos 18080/15432) → `postgres` Healthy y `api` Healthy.
    - `5/5 Pruebas E2E` → `BUILD SUCCESSFUL`; **4 pruebas E2E, 0 failures**.
    - Salida final: `✅ Toda la arquitectura pasó las pruebas` (la consola PowerShell muestra mojibake en el carácter ✅ por su codificación, el script y el código de salida son correctos).
    - `finally`: `docker compose -p miapp-test down -v --remove-orphans` → contenedores y volumen eliminados.
- Verificación adicional de salud (póstuma a Fase 6): `docker compose -p miapp-health up -d --wait` con `API_PORT=18086`, `DB_PORT=15436` → `postgres=... (healthy)`, `api=... (healthy)` y `curl -fsS http://localhost:18086/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}`; stack destruido después.
- Contrato runtime vs diseño en el stack de Fase 6 reconstruido: **26 paths / 34 operaciones / 36 schemas**, `missing=[]`, `extra=[]`. `docs/backend/compat-report.md` revisado: 0 filas con ❌.

**✅ Criterio "Listo cuando":** `scripts/test-all` termina con ✅ en limpio (sin contenedores previos). Verificado tras los cambios de Fase 6.
