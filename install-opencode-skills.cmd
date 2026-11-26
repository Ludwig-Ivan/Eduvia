<# :
@echo off
setlocal
rem Instala skills de opencode: app KMP -> API REST Spring Boot + PostgreSQL
rem Uso:  install-opencode-skills.cmd [ruta_proyecto]
rem       set "FORCE=1"  y luego  install-opencode-skills.cmd   (sobrescribe skills existentes)
set "OC_PROJECT=%~1"
set "OC_SELF=%~f0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression ([IO.File]::ReadAllText($env:OC_SELF, [Text.Encoding]::UTF8))"
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
#>
$ErrorActionPreference = 'Stop'
$project = if ($env:OC_PROJECT) { $env:OC_PROJECT } else { (Get-Location).Path }
if (-not (Test-Path -LiteralPath $project -PathType Container)) { Write-Host "No existe el directorio: $project"; exit 1 }
$project = (Resolve-Path -LiteralPath $project).Path
$dest = Join-Path $project '.opencode\skills'
$force = ($env:FORCE -eq '1')
$utf8 = New-Object System.Text.UTF8Encoding($false)
$script:installed = 0; $script:skipped = 0
function Write-SkillFile([string]$rel, [string]$content) {
  $target = Join-Path $dest ($rel -replace '/', '\')
  if ((Test-Path -LiteralPath $target) -and -not $force) {
    Write-Host "  - omitido (ya existe): $rel"; $script:skipped++; return
  }
  $dir = Split-Path -Parent $target
  if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $text = $content.Replace("`r`n", "`n")
  if (-not $text.EndsWith("`n")) { $text += "`n" }
  [System.IO.File]::WriteAllText($target, $text, $utf8)
  $script:installed++
}
Write-SkillFile 'backend-docker-compose/SKILL.md' @'
---
name: backend-docker-compose
description: Crea el entorno local y la imagen de despliegue del backend - Dockerfile multi-stage para Spring Boot, docker-compose con PostgreSQL y la API, variables de entorno, healthchecks y cómo conectar el emulador Android/simulador iOS al backend local. Úsala para levantar o contenerizar el backend.
license: MIT
compatibility: opencode
---

# Docker y entorno local

## 1. `backend/compose.yaml` (solo Postgres, para desarrollo con `bootRun`)

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      retries: 10
volumes:
  pgdata:
```

Con la dependencia `spring-boot-docker-compose`, `./gradlew bootRun` levanta este compose automáticamente y configura el datasource.

## 2. `backend/Dockerfile` (multi-stage)

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=build /src/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
```

Ajusta la versión de Java a la del `toolchain` del proyecto. Añade `.dockerignore` (`build/`, `.gradle/`, `*.iml`, `.env`).

## 3. `docker-compose.yml` en la raíz (stack completo)

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-app}
      POSTGRES_USER: ${DB_USER:-app}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-app}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-app} -d ${DB_NAME:-app}"]
      interval: 5s
      retries: 10
  api:
    build: ./backend
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-app}
      DB_USER: ${DB_USER:-app}
      DB_PASSWORD: ${DB_PASSWORD:-app}
      JWT_SECRET: ${JWT_SECRET:?define JWT_SECRET en .env}   # quitar si no hay auth
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DOCKER_COMPOSE_ENABLED: "false"
    ports: ["8080:8080"]
volumes:
  pgdata:
```

Crea `.env.example` con las variables (sin secretos reales) y añade `.env` al `.gitignore`.

## 4. Conectar la app KMP al backend local

| Plataforma | Host |
|---|---|
| Emulador Android | `http://10.0.2.2:8080` |
| Simulador iOS | `http://localhost:8080` |
| Dispositivo físico | IP LAN del equipo, p. ej. `http://192.168.1.50:8080` |

- Android con HTTP en claro requiere `network_security_config` permitiendo ese host (solo en debug).
- iOS requiere excepción ATS en `Info.plist` para HTTP en desarrollo.
- No cambies la configuración de la app sin confirmación (eso es del paso `kmp-client-sync`).

## 5. Verificación

```bash
docker compose up -d --build
curl -fsS http://localhost:8080/actuator/health   # {"status":"UP"}
docker compose logs api | grep -i flyway           # migraciones aplicadas
```
'@
Write-SkillFile 'kmp-api-contract/SKILL.md' @'
---
name: kmp-api-contract
description: Analiza el código de una app Kotlin Multiplatform (Ktor client, Ktorfit, kotlinx.serialization, SQLDelight/Room, repositorios) y extrae el contrato de API que espera consumir - endpoints, métodos HTTP, DTOs, auth, paginación y errores. Genera api-contract.md y openapi.yaml. Úsala antes de diseñar cualquier backend para una app KMP.
license: MIT
compatibility: opencode
---

# Extraer el contrato de API desde una app KMP

## 1. Escaneo inicial

Ejecuta el script incluido desde la raíz del proyecto (ruta relativa a esta skill):

```bash
# Linux / macOS / Git Bash / WSL
bash .opencode/skills/kmp-api-contract/scripts/scan-kmp.sh .
```

```powershell
# Windows (PowerShell o cmd)
powershell -NoProfile -ExecutionPolicy Bypass -File .opencode\skills\kmp-api-contract\scripts\scan-kmp.ps1 .
```

Te da una lista de archivos candidatos. Luego **lee los archivos** (no te quedes con el grep).

## 2. Qué buscar

| Elemento | Dónde suele estar | Qué extraer |
|---|---|---|
| Base URL | `HttpClient { defaultRequest { url(...) } }`, `BuildConfig`, `expect/actual`, constantes | Prefijo (`/api/v1`), host por entorno |
| Llamadas HTTP | `client.get/post/put/patch/delete(...)`, interfaces Ktorfit (`@GET`, `@POST`...) | Método, ruta, path params, query params, body, tipo de respuesta |
| DTOs | clases `@Serializable` en `commonMain` (`data`, `remote`, `dto`, `network`) | Campos, tipos, nulabilidad, valores por defecto, `@SerialName` |
| Config JSON | `Json { ... }` | `ignoreUnknownKeys`, `explicitNulls`, `namingStrategy`, `encodeDefaults` |
| Auth | plugin `Auth { bearer { ... } }`, headers `Authorization`, refresh token | Esquema, endpoint de login/refresh, formato de token |
| Paginación | params `page`, `size`, `cursor`, `limit`, `offset`; DTOs `PagedResponse` | Estilo y forma de la respuesta |
| Errores | `HttpResponseValidator`, `ClientRequestException`, DTOs `ErrorResponse` | Forma del cuerpo de error y códigos que la app maneja |
| Persistencia local | `.sq` de SQLDelight, `@Entity` de Room | Pistas del modelo de datos y relaciones (no es el contrato, pero ayuda al esquema) |
| Fechas | `kotlinx.datetime.Instant/LocalDate`, `String` con formato | Formato exacto esperado (ISO-8601 recomendado) |

## 3. Reglas de interpretación

- **Nulabilidad**: un campo `String` (no nulo) sin valor por defecto en un DTO de respuesta es **obligatorio** en la API: si el backend no lo manda, kotlinx.serialization lanza excepción.
- **Nombres**: el nombre JSON es el de `@SerialName` si existe; si no, el nombre de la propiedad. Si hay `namingStrategy = JsonNamingStrategy.SnakeCase`, todo va en snake_case.
- **Enums**: registra los valores exactos (incluido `@SerialName` en cada constante).
- **IDs**: anota si son `String` (probable UUID), `Long` o `Int`.
- Si un endpoint se usa pero su respuesta se ignora (`Unit`/`HttpResponse`), márcalo y propón `204 No Content`.
- Si `ignoreUnknownKeys = false`, el backend **no puede** añadir campos extra en las respuestas: anótalo como restricción crítica.

## 4. Entregables

### `docs/backend/api-contract.md`
Por cada endpoint:

```markdown
### POST /api/v1/orders
- Origen en la app: `shared/src/commonMain/kotlin/.../OrderApi.kt:42`
- Auth: Bearer requerido
- Request: `CreateOrderRequest` { customerId: UUID, items: [OrderItemDto] (min 1) }
- Response 201: `OrderDto`
- Errores que la app maneja: 400, 401, 409
- Notas/dudas: ...
```

Incluye al inicio una sección **Convenciones globales** (base path, naming JSON, formato de fechas, paginación, formato de error, config `Json {}` detectada).

### `docs/backend/openapi.yaml`
OpenAPI 3.0.3 (o 3.1) con todos los paths y `components/schemas` derivados de los DTOs, respetando nombres, `required` y `nullable` según las reglas de arriba.

## 5. Verificación

- Cada llamada HTTP encontrada en la app aparece en el contrato (haz un conteo y compáralo).
- Cada DTO usado en request/response tiene schema.
- Lista de dudas en `docs/backend/OPEN-QUESTIONS.md`.
'@
Write-SkillFile 'kmp-api-contract/scripts/scan-kmp.ps1' @'
# Escanea una app Kotlin Multiplatform y lista archivos/lineas relevantes
# para reconstruir el contrato de API (version Windows / PowerShell).
# Uso: powershell -ExecutionPolicy Bypass -File scan-kmp.ps1 [raiz_proyecto]
param([string]$Root = ".")

$exclude = '\\(build|\.gradle|\.git|node_modules|backend|\.opencode)\\'
function Get-Files($pattern) {
  Get-ChildItem -Path $Root -Recurse -File -Filter $pattern -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch $exclude }
}
function Section($t) { Write-Output ""; Write-Output "==================== $t ====================" }
function Search($regex, $pattern) {
  Get-Files $pattern | Select-String -Pattern $regex -CaseSensitive |
    ForEach-Object { "{0}:{1}: {2}" -f $_.Path, $_.LineNumber, $_.Line.Trim() }
}

$kt = "*.kt"

Section "Modulos y targets KMP"
Get-Files "build.gradle.kts" | Select-String -Pattern 'kotlin\("multiplatform"\)|kotlinMultiplatform' -List | ForEach-Object { $_.Path }

Section "Dependencias de red/serializacion/persistencia"
Search 'ktor-client|ktorfit|kotlinx-serialization|sqldelight|androidx.room|kotlinx-datetime' "*.kts"
Search 'ktor|ktorfit|serialization|sqldelight|room|datetime' "*.toml"

Section "Configuracion del HttpClient / base URL"
Search 'HttpClient\(|defaultRequest|BASE_URL|baseUrl|base_url|URLProtocol|install\((Auth|ContentNegotiation|HttpTimeout)' $kt

Section "Llamadas HTTP (Ktor)"
Search '\.(get|post|put|patch|delete|head)\(|\.request\(|HttpMethod\.' $kt

Section "Interfaces Ktorfit / estilo Retrofit"
Search '@(GET|POST|PUT|PATCH|DELETE|HTTP)\(' $kt

Section "Clases @Serializable (DTOs candidatos)"
Get-Files $kt | Select-String -Pattern '@Serializable' -CaseSensitive | Group-Object Path |
  Sort-Object Count -Descending | ForEach-Object { "{0,5} {1}" -f $_.Count, $_.Name }

Section "@SerialName y configuracion Json"
Search '@SerialName|Json \{|ignoreUnknownKeys|explicitNulls|namingStrategy|encodeDefaults' $kt

Section "Autenticacion"
Search 'bearer|Bearer|Authorization|refreshTokens|loadTokens|BearerTokens|apiKey|X-API-Key' $kt

Section "Manejo de errores"
Search 'HttpResponseValidator|ClientRequestException|ServerResponseException|ResponseException|status\.value|HttpStatusCode\.' $kt

Section "Paginacion"
Search 'page|pageSize|cursor|offset|limit|nextPage|hasMore|totalElements' $kt |
  Where-Object { $_ -match '(?i)param|query|class|val ' } | Select-Object -First 80

Section "Persistencia local (pistas de modelo)"
Get-Files "*.sq" | ForEach-Object { $_.FullName }
Search '@Entity|@Dao|@PrimaryKey|@ForeignKey' $kt

Write-Output ""
Write-Output "Listo. Lee los archivos listados para extraer el contrato completo."
'@
Write-SkillFile 'kmp-api-contract/scripts/scan-kmp.sh' @'
#!/usr/bin/env bash
# Escanea una app Kotlin Multiplatform y lista archivos/lineas relevantes
# para reconstruir el contrato de API. Uso: bash scan-kmp.sh [raiz_proyecto]
set -euo pipefail
ROOT="${1:-.}"

EXCLUDES=(--exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git \
          --exclude-dir=node_modules --exclude-dir=backend --exclude-dir=.opencode)

section() { printf '\n==================== %s ====================\n' "$1"; }
search() { grep -rnE "${EXCLUDES[@]}" --include="$2" "$1" "$ROOT" 2>/dev/null || true; }

section "Modulos y targets KMP"
find "$ROOT" -name "build.gradle.kts" -not -path "*/build/*" -not -path "*/backend/*" \
  -exec grep -lE "kotlin\(\"multiplatform\"\)|kotlinMultiplatform" {} \; 2>/dev/null || true

section "Dependencias de red/serializacion/persistencia"
search "ktor-client|ktorfit|kotlinx-serialization|sqldelight|androidx.room|kotlinx-datetime" "*.kts"
search "ktor|ktorfit|serialization|sqldelight|room|datetime" "*.toml"

section "Configuracion del HttpClient / base URL"
search "HttpClient\(|defaultRequest|BASE_URL|baseUrl|base_url|URLProtocol|install\((Auth|ContentNegotiation|HttpTimeout)" "*.kt"

section "Llamadas HTTP (Ktor)"
search "\.(get|post|put|patch|delete|head)\(|\.request\(|HttpMethod\." "*.kt"

section "Interfaces Ktorfit / estilo Retrofit"
search "@(GET|POST|PUT|PATCH|DELETE|HTTP)\(" "*.kt"

section "Clases @Serializable (DTOs candidatos)"
search "@Serializable" "*.kt" | cut -d: -f1 | sort | uniq -c | sort -rn

section "@SerialName y configuracion Json"
search "@SerialName|Json \{|ignoreUnknownKeys|explicitNulls|namingStrategy|encodeDefaults" "*.kt"

section "Autenticacion"
search "bearer|Bearer|Authorization|refreshTokens|loadTokens|BearerTokens|apiKey|X-API-Key" "*.kt"

section "Manejo de errores"
search "HttpResponseValidator|ClientRequestException|ServerResponseException|ResponseException|status\.value|HttpStatusCode\." "*.kt"

section "Paginacion"
search "page|pageSize|cursor|offset|limit|nextPage|hasMore|totalElements" "*.kt" | grep -iE "param|query|class|val " | head -n 80 || true

section "Persistencia local (pistas de modelo)"
find "$ROOT" -name "*.sq" -not -path "*/build/*" 2>/dev/null || true
search "@Entity|@Dao|@PrimaryKey|@ForeignKey" "*.kt"

printf '\nListo. Lee los archivos listados para extraer el contrato completo.\n'
'@
Write-SkillFile 'kmp-backend-orchestrator/SKILL.md' @'
---
name: kmp-backend-orchestrator
description: Punto de entrada para generar un backend REST en Spring Boot con base de datos PostgreSQL a partir de una app móvil Kotlin Multiplatform (KMP) existente. Úsala cuando el usuario pida "crear el backend/API para la app", "generar la API desde la app KMP" o similar. Coordina el orden de las demás skills.
license: MIT
compatibility: opencode
---

# Orquestador: app KMP → API Spring Boot + PostgreSQL

Esta skill define el flujo completo. Cada paso tiene su propia skill: cárgala con la herramienta `skill` antes de ejecutarlo.

## Antes de empezar: decisiones (preguntar al usuario si no están claras)

1. **Ubicación del backend**: carpeta `backend/` dentro del mismo repo (por defecto) o repo separado.
2. **Lenguaje del backend**: Kotlin (por defecto, coherente con KMP) o Java.
3. **Autenticación**: ¿la app envía tokens (Bearer/JWT), API keys, o nada? Detectarlo en el paso 1 y confirmar.
4. **Paquete base**: p. ej. `com.empresa.app.api` (derivar del `applicationId`/namespace de la app si existe).

Si el usuario no está disponible, usa los valores por defecto y anótalos en `docs/backend/DECISIONS.md`.

## Flujo (en este orden)

| # | Skill | Entregable |
|---|-------|-----------|
| 1 | `kmp-api-contract` | `docs/backend/api-contract.md` + `docs/backend/openapi.yaml` |
| 2 | `postgres-schema-design` | `docs/backend/data-model.md` + migraciones Flyway |
| 3 | `springboot-project-setup` | Proyecto `backend/` compilando |
| 4 | `springboot-jpa-postgres` | Entidades, repositorios, migraciones aplicadas |
| 5 | `springboot-rest-api` | Controllers, DTOs, validación, errores |
| 6 | `springboot-security-jwt` | Solo si la app usa autenticación |
| 7 | `springboot-testing` | Tests de integración con Testcontainers en verde |
| 8 | `backend-docker-compose` | `docker compose up` levanta Postgres + API |
| 9 | `kmp-client-sync` | Informe de compatibilidad app ↔ API (y cambios opcionales en la app) |

## Reglas generales

- **El contrato manda.** La fuente de verdad es lo que la app KMP ya envía y espera recibir. El backend se adapta a la app, no al revés, salvo que el usuario lo autorice.
- **No modificar la app KMP** hasta el paso 9, y solo con confirmación explícita.
- Después de cada paso: compilar (`./gradlew build -x test` en `backend/`) y, desde el paso 7, correr tests. No avanzar con el build roto.
- No inventar versiones de librerías: usar las que da start.spring.io o las verificadas en Maven Central.
- **Windows**: los ejemplos de comandos están en sintaxis bash. En Windows usa `gradlew.bat` en lugar de `./gradlew`, PowerShell para scripts (p. ej. `scan-kmp.ps1`) y `curl.exe` en vez del alias `curl` de PowerShell. Docker Desktop debe estar corriendo para Testcontainers y docker compose.
- Mantener un registro breve en `docs/backend/PROGRESS.md` (paso, estado, pendientes) para poder retomar.
- Si algo del contrato es ambiguo (campo sin uso claro, endpoint sin respuesta visible), anotarlo en `docs/backend/OPEN-QUESTIONS.md` en vez de suponer en silencio.

## Criterio de terminado

- `docker compose up` levanta todo; `GET /actuator/health` responde `UP`.
- Todos los endpoints del contrato existen y tienen al menos un test de integración.
- Swagger UI en `/swagger-ui.html` coincide con `docs/backend/openapi.yaml`.
- El informe de `kmp-client-sync` no tiene incompatibilidades abiertas.
'@
Write-SkillFile 'kmp-client-sync/SKILL.md' @'
---
name: kmp-client-sync
description: Verifica y sincroniza la app Kotlin Multiplatform con la API Spring Boot generada - compara rutas, DTOs, nulabilidad, enums, fechas y errores, produce un informe de compatibilidad y, con confirmación, ajusta base URL, DTOs o configuración de red de la app. Úsala al terminar o cambiar el backend.
license: MIT
compatibility: opencode
---

# Sincronizar app KMP ↔ API

## 1. Fuentes a comparar

- App: llamadas Ktor/Ktorfit y DTOs `@Serializable` (re-escanear con `.opencode/skills/kmp-api-contract/scripts/scan-kmp.sh`, o `scan-kmp.ps1` en Windows).
- Backend: OpenAPI real generado por springdoc (`curl http://localhost:8080/v3/api-docs.yaml`) — no solo el `openapi.yaml` de diseño.

## 2. Checklist por endpoint

- [ ] Método y ruta idénticos (incluido prefijo `/api/v1` y barras finales).
- [ ] Path y query params con el mismo nombre y tipo.
- [ ] Request: todos los campos que envía la app los acepta el backend con el mismo nombre.
- [ ] Response: todo campo **no nulo sin default** en el DTO de la app siempre viene en la respuesta.
- [ ] Sin campos extra si la app tiene `ignoreUnknownKeys = false`.
- [ ] Enums: mismos literales.
- [ ] Fechas: formato que la app parsea (`Instant` → ISO-8601 con `Z` u offset).
- [ ] Números: `Long` vs `Int` (ids grandes), decimales como número o string según la app.
- [ ] Códigos de estado que la app maneja (`201`, `204`, `404`, `409`...) y formato de error.
- [ ] Auth: 401 + `WWW-Authenticate: Bearer` para que el refresh de Ktor funcione.

## 3. Informe `docs/backend/compat-report.md`

| Endpoint | Estado | Problema | Lado a corregir | Propuesta |
|---|---|---|---|---|
| `GET /api/v1/orders/{id}` | ❌ | `createdAt` llega como epoch ms; app espera ISO | Backend | `write-dates-as-timestamps: false` |

Por defecto se corrige el **backend**. Solo se propone cambiar la app si el contrato de la app es claramente defectuoso o el usuario lo pide.

## 4. Cambios en la app (solo con confirmación explícita)

- Base URL por entorno (`expect/actual` o BuildKonfig) apuntando al backend local/producción.
- Config de red de desarrollo: `network_security_config` (Android) y ATS (iOS) para HTTP local.
- Si se acordó compartir DTOs: mover DTOs a un módulo `shared-api` que pueda consumir también el backend (solo si el backend es Kotlin/JVM y el usuario lo aprueba).

## 5. Prueba extremo a extremo

1. `docker compose up -d`.
2. Ejecutar la app en emulador/simulador contra el backend local.
3. Recorrer los flujos principales (login, listados, crear, editar, borrar) y revisar `docker compose logs -f api` buscando 4xx/5xx inesperados.
4. Si existen tests de la capa de red en la app (`commonTest` con MockEngine), considerar añadir fixtures con respuestas reales del backend.

Terminado cuando el informe no tiene filas ❌.
'@
Write-SkillFile 'postgres-schema-design/SKILL.md' @'
---
name: postgres-schema-design
description: Diseña el esquema PostgreSQL (tablas, tipos, claves, índices, restricciones) a partir del contrato de API y los DTOs de una app KMP, y lo escribe como migraciones Flyway versionadas. Úsala al crear o cambiar el modelo de datos del backend.
license: MIT
compatibility: opencode
---

# Diseño de esquema PostgreSQL + migraciones Flyway

Entrada: `docs/backend/api-contract.md` y `openapi.yaml`. Pistas adicionales: archivos `.sq` (SQLDelight) o `@Entity` (Room) de la app.

## 1. De DTOs a entidades

- Un DTO de API **no** es una tabla automáticamente. Identifica entidades reales (sustantivos con identidad y ciclo de vida) y separa:
  - Objetos anidados con identidad propia → tabla con FK.
  - Objetos anidados sin identidad (dirección, coordenadas) → columnas embebidas o `jsonb` si son variables.
  - Listas de primitivos (`List<String>` de tags) → tabla hija, o `text[]` si nunca se consultan por separado.
  - Campos calculados (totales, contadores) → no se guardan salvo que haya motivo de rendimiento; documentarlo.

## 2. Mapeo de tipos

| Kotlin (DTO) | PostgreSQL | Nota |
|---|---|---|
| `String` id | `uuid` | `default gen_random_uuid()` (PG 13+) |
| `Long` id | `bigint generated always as identity` | |
| `String` | `text` o `varchar(n)` | `varchar(n)` solo si hay límite de negocio real |
| `Int` / `Long` | `integer` / `bigint` | |
| `Double` | `double precision` | **Dinero → `numeric(19,4)`**, nunca double |
| `Boolean` | `boolean not null default false` | |
| `Instant` | `timestamptz` | Siempre con zona |
| `LocalDate` | `date` | |
| `LocalDateTime` | `timestamp` | Evitar si se puede; preferir `timestamptz` |
| `enum class` | `varchar(32)` + `check (col in (...))` | Más fácil de migrar que un `enum` de PG |
| Objeto libre / mapa | `jsonb` | Solo si la estructura es realmente variable |

Nulabilidad: si el campo es no nulo en el DTO de respuesta → `not null` en la columna.

## 3. Convenciones

- Nombres en `snake_case`, tablas en **plural** (`orders`, `order_items`).
- PK: `id`. FK: `<entidad_singular>_id` con `references ... on delete` explícito (`cascade` para composición, `restrict` para referencias).
- Toda tabla de negocio: `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`.
- Optimistic locking opcional: `version bigint not null default 0`.
- Soft delete solo si la app lo necesita (`deleted_at timestamptz`).
- Restricciones `unique` para claves naturales (email, sku). Nombrar constraints: `uk_users_email`, `fk_orders_customer`, `ck_orders_status`.

## 4. Índices

- Índice en **cada FK**.
- Índices para cada filtro/orden que aparezca como query param en el contrato (`?status=`, `?sort=createdAt`).
- Búsqueda de texto con `ILIKE '%x%'` → considerar `pg_trgm` + índice GIN (extensión en su propia migración).
- No indexar de más; justificar cada índice en `data-model.md`.

## 5. Migraciones Flyway

Ubicación: `backend/src/main/resources/db/migration/`

- `V1__init_schema.sql` – extensiones + tablas base.
- `V2__...sql`, `V3__...sql` – un cambio lógico por archivo.
- **Nunca editar una migración ya aplicada**; crear una nueva.
- Datos de ejemplo para desarrollo: `src/main/resources/db/dev-data/` y activarlos solo con perfil `dev` (`spring.flyway.locations` por perfil), nunca en producción.

Plantilla:

```sql
create extension if not exists pgcrypto;

create table customers (
    id          uuid primary key default gen_random_uuid(),
    email       varchar(320) not null,
    full_name   text not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint uk_customers_email unique (email)
);
```

## 6. Entregable `docs/backend/data-model.md`

- Diagrama ER en Mermaid (`erDiagram`).
- Tabla por entidad: columna, tipo, null, default, origen (campo DTO).
- Mapeo DTO ↔ tabla, señalando campos calculados o derivados.
- Lista de índices con justificación.

## 7. Verificación

Levanta un Postgres temporal y aplica las migraciones:

```bash
docker run --rm -d --name pg-check -e POSTGRES_PASSWORD=pg -p 55432:5432 postgres:17
# esperar a que acepte conexiones, luego:
for f in backend/src/main/resources/db/migration/V*.sql; do
  docker exec -i pg-check psql -U postgres -v ON_ERROR_STOP=1 < "$f"; done
docker stop pg-check
```

O bien, mejor, deja que los tests con Testcontainers (skill `springboot-testing`) apliquen Flyway.
'@
Write-SkillFile 'springboot-jpa-postgres/SKILL.md' @'
---
name: springboot-jpa-postgres
description: Implementa la capa de persistencia de Spring Boot sobre PostgreSQL con Spring Data JPA/Hibernate y Flyway - entidades alineadas con las migraciones, repositorios, consultas, relaciones, auditoría y transacciones, evitando N+1. Úsala al crear o cambiar entidades y repositorios.
license: MIT
compatibility: opencode
---

# Persistencia: Spring Data JPA + PostgreSQL

Entrada: migraciones de `postgres-schema-design` y `docs/backend/data-model.md`. El esquema lo manda Flyway; las entidades se ajustan a él (`ddl-auto: validate` lo comprueba al arrancar).

## 1. Entidades (Kotlin)

```kotlin
@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    var customer: CustomerEntity,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItemEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    var version: Long = 0,
)
```

Reglas:
- **No usar `data class`** para entidades (equals/hashCode/toString rompen con proxies y relaciones). Clase normal; `equals/hashCode` basados en `id` si hace falta.
- `@Enumerated(EnumType.STRING)` siempre, nunca ORDINAL.
- `@ManyToOne(fetch = LAZY)` siempre (por defecto es EAGER).
- Dinero: `BigDecimal` con `@Column(precision = 19, scale = 4)`.
- `jsonb`: `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6+).
- Arrays `text[]`: `@JdbcTypeCode(SqlTypes.ARRAY)`.
- Auditoría: `@PrePersist/@PreUpdate` o `@EnableJpaAuditing` + `@CreatedDate/@LastModifiedDate`.

En Java: misma idea; constructor protegido sin args, getters/setters, sin Lombok `@Data` en entidades.

## 2. Repositorios

```kotlin
interface OrderRepository : JpaRepository<OrderEntity, UUID> {
    fun findAllByCustomerId(customerId: UUID, pageable: Pageable): Page<OrderEntity>

    @EntityGraph(attributePaths = ["items", "customer"])
    fun findWithItemsById(id: UUID): OrderEntity?

    fun existsByCustomerIdAndStatus(customerId: UUID, status: OrderStatus): Boolean
}
```

- Métodos derivados para consultas simples; `@Query` JPQL para las complejas; `nativeQuery = true` solo para funciones específicas de PG (full-text, `jsonb` ops).
- Filtros dinámicos (muchos query params opcionales): `JpaSpecificationExecutor` + `Specification`.
- Proyecciones (interfaces o DTO constructors) para listados grandes.

## 3. Evitar N+1

- Listados que muestran relaciones → `@EntityGraph` o `join fetch`.
- No paginar con `join fetch` sobre colecciones (Hibernate pagina en memoria); en su lugar: paginar IDs y luego cargar con fetch, o `@BatchSize`.
- Activa en `dev`: `spring.jpa.properties.hibernate.generate_statistics: true` o logs SQL para revisar.

## 4. Transacciones

- `@Transactional` en **servicios**, no en controllers ni repositorios.
- Lecturas: `@Transactional(readOnly = true)`.
- Mapear a DTO **dentro** de la transacción (con `open-in-view: false`, fuera daría `LazyInitializationException`).
- `OptimisticLockingFailureException` → 409 en el handler global.

## 5. Verificación

- La app arranca con `ddl-auto: validate` sin errores (las entidades coinciden con Flyway).
- Tests de repositorio con `@DataJpaTest` + Testcontainers (ver `springboot-testing`), **no H2** (tipos de PG como `jsonb`, `uuid`, `timestamptz` se comportan distinto).
'@
Write-SkillFile 'springboot-project-setup/SKILL.md' @'
---
name: springboot-project-setup
description: Crea el esqueleto de un proyecto Spring Boot (Kotlin o Java, Gradle Kotlin DSL) para una API REST con PostgreSQL, Flyway, validación, Actuator y OpenAPI usando start.spring.io. Úsala al iniciar el backend o al reorganizar su estructura y configuración.
license: MIT
compatibility: opencode
---

# Setup del proyecto Spring Boot

## 1. Generar con Spring Initializr (no escribir el build a mano)

Desde la raíz del repo (ajusta `groupId`/`packageName`):

```bash
curl -fsSL https://start.spring.io/starter.zip \
  -d type=gradle-project-kotlin \
  -d language=kotlin \
  -d javaVersion=21 \
  -d groupId=com.example.app \
  -d artifactId=api \
  -d name=api \
  -d packageName=com.example.app.api \
  -d dependencies=web,validation,data-jpa,postgresql,flyway,actuator,docker-compose,testcontainers \
  -o /tmp/backend.zip && unzip -q /tmp/backend.zip -d backend && rm /tmp/backend.zip
```

- Si el usuario eligió Java: `language=java`.
- Si habrá autenticación JWT añade `security,oauth2-resource-server`.
- Usa la versión de Spring Boot que start.spring.io da por defecto (la estable actual). No la fijes a mano.
- Si no hay red, crea el `build.gradle.kts` equivalente pero **pregunta/verifica** versiones en Maven Central; nunca las inventes.

## 2. Añadir OpenAPI (springdoc)

`springdoc-openapi-starter-webmvc-ui` no está en Initializr. Añádelo manualmente comprobando en https://springdoc.org la línea compatible con la versión de Spring Boot generada (springdoc 2.x ↔ Boot 3.x; 3.x ↔ Boot 4.x).

## 3. Estructura de paquetes (por feature)

```
backend/src/main/kotlin/com/example/app/api/
├── ApiApplication.kt
├── common/            # errores, paginación, config compartida
│   ├── error/         # GlobalExceptionHandler, excepciones de dominio
│   └── web/           # PageResponse, config Jackson/CORS
├── config/            # SecurityConfig, OpenApiConfig
└── <feature>/         # orders/, customers/, ...
    ├── <Feature>Controller.kt
    ├── <Feature>Service.kt
    ├── <Feature>Repository.kt
    ├── <Feature>Entity.kt
    ├── dto/           # Request/Response DTOs
    └── <Feature>Mapper.kt
```

## 4. Configuración

`src/main/resources/application.yml`:

```yaml
spring:
  application.name: api
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/app}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:app}
  jpa:
    open-in-view: false
    hibernate.ddl-auto: validate   # Flyway crea el esquema; Hibernate solo valida
    properties.hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
server:
  port: ${PORT:8080}
  error.include-stacktrace: never
management:
  endpoints.web.exposure.include: health,info
```

Perfiles: `application-dev.yml` (logs SQL, datos de ejemplo), `application-prod.yml` (sin defaults de credenciales).

Reglas:
- **Nunca** `ddl-auto: update/create` – el esquema es de Flyway.
- Secretos solo por variables de entorno; añade `.env` al `.gitignore`.
- `open-in-view: false` siempre.

## 5. Kotlin + JPA

Initializr ya incluye `kotlin("plugin.spring")` y `kotlin("plugin.jpa")`. Verifica además:

```kotlin
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

y que exista `jackson-module-kotlin` (Initializr lo añade con `web`).

## 6. Verificación

```bash
cd backend && ./gradlew build -x test
```

Debe compilar. Arrancar requiere Postgres (ver `backend-docker-compose`).
'@
Write-SkillFile 'springboot-rest-api/SKILL.md' @'
---
name: springboot-rest-api
description: Implementa endpoints REST en Spring Boot que cumplen exactamente el contrato que consume una app KMP - controllers, DTOs, validación, paginación, serialización JSON compatible con kotlinx.serialization y errores con ProblemDetail (RFC 9457). Úsala para crear o modificar controllers y DTOs.
license: MIT
compatibility: opencode
---

# Endpoints REST compatibles con la app KMP

Fuente de verdad: `docs/backend/api-contract.md` / `openapi.yaml`.

## 1. Capas

- **Controller**: solo HTTP (mapeo, validación `@Valid`, códigos de estado). Sin lógica de negocio ni acceso a repositorios.
- **Service**: lógica de negocio, `@Transactional` (lectura: `@Transactional(readOnly = true)`).
- **Mapper**: Entity ↔ DTO (funciones de extensión en Kotlin o MapStruct en Java). **Nunca exponer entidades JPA** en la API.

## 2. DTOs

- Nombres de campos JSON **idénticos** a la app (`@SerialName` incluido). Si la app usa snake_case global, configura Jackson:
  ```yaml
  spring.jackson.property-naming-strategy: SNAKE_CASE
  ```
  Si es solo en algunos campos, usa `@JsonProperty("...")` en esos.
- Requests: validación con Jakarta Validation (`@field:NotBlank`, `@field:Size`, `@field:Email`, `@field:Positive`... en Kotlin usar el prefijo `@field:`).
- Responses: todo campo no nulo en la app debe enviarse siempre. Si la app tiene `explicitNulls = false` o valores por defecto, puedes omitir nulos con `spring.jackson.default-property-inclusion: non_null`; si no, mantenlos.
- Fechas: ISO-8601 (`2026-01-31T12:00:00Z`). Asegura `spring.jackson.serialization.write-dates-as-timestamps: false`. Usa `Instant`/`OffsetDateTime` en DTOs.
- Enums: mismos literales que la app.
- Si la app tiene `ignoreUnknownKeys = false`, **no añadas** campos que la app no conoce.

## 3. Convenciones HTTP

| Operación | Método | Éxito |
|---|---|---|
| Listar | `GET /recursos` | 200 + página |
| Obtener | `GET /recursos/{id}` | 200 / 404 |
| Crear | `POST /recursos` | 201 + header `Location` + cuerpo |
| Reemplazar | `PUT /recursos/{id}` | 200 |
| Parcial | `PATCH /recursos/{id}` | 200 |
| Borrar | `DELETE /recursos/{id}` | 204 |

**Si el contrato de la app dice otra cosa, gana el contrato.**

## 4. Paginación

Replica la forma que espera la app. Si no hay ninguna, usa:

```kotlin
data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
```

No devuelvas `Page<T>` de Spring directamente (su JSON es inestable entre versiones). Limita `size` (p. ej. máx. 100).

## 5. Errores (ProblemDetail, RFC 9457)

Activa `spring.mvc.problemdetails.enabled: true` y crea un `@RestControllerAdvice`:

- `NotFoundException` → 404
- `ConflictException` (duplicados, `DataIntegrityViolationException` de unique) → 409
- `MethodArgumentNotValidException` → 400 con `errors: [{field, message}]` en `properties`
- `HttpMessageNotReadableException` → 400
- `Exception` genérica → 500 sin detalles internos (log con stacktrace en servidor)

Si la app ya espera otro formato de error (`ErrorResponse { code, message }`), **usa ese formato** en el handler.

## 6. Plantilla (Kotlin)

```kotlin
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val service: OrderService) {

    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "20") size: Int): PageResponse<OrderDto> =
        service.list(page, size.coerceAtMost(100))

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): OrderDto = service.get(id)

    @PostMapping
    fun create(@Valid @RequestBody req: CreateOrderRequest): ResponseEntity<OrderDto> {
        val created = service.create(req)
        return ResponseEntity.created(URI.create("/api/v1/orders/${created.id}")).body(created)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
```

## 7. CORS

Solo necesario si también hay cliente web (Compose for Web/JS). Las apps Android/iOS nativas no aplican CORS. Configurar orígenes explícitos, nunca `*` con credenciales.

## 8. Verificación

- Cada endpoint de `api-contract.md` existe (mismo método y ruta).
- Swagger UI (`/swagger-ui.html`) refleja los mismos schemas que `openapi.yaml`.
- Tests de controller/integración (skill `springboot-testing`).
'@
Write-SkillFile 'springboot-security-jwt/SKILL.md' @'
---
name: springboot-security-jwt
description: Configura autenticación y autorización en una API Spring Boot para apps móviles KMP - login, emisión y validación de JWT (access + refresh), hash de contraseñas, rutas públicas/protegidas y compatibilidad con el plugin Auth bearer de Ktor. Úsala cuando la app envíe tokens o necesite usuarios.
license: MIT
compatibility: opencode
---

# Seguridad JWT para clientes móviles

Usar solo si el contrato (`kmp-api-contract`) muestra autenticación. Replica lo que la app ya hace: rutas de login/refresh, nombres de campos (`accessToken` vs `access_token`), y cómo se envía el token.

## 1. Dependencias

`security` y `oauth2-resource-server` (Initializr). Esto da validación de JWT nativa con Nimbus; no añadas librerías JWT de terceros salvo necesidad.

## 2. Modelo

Migración Flyway:

```sql
create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null,
    password_hash text not null,
    role varchar(32) not null default 'USER',
    created_at timestamptz not null default now(),
    constraint uk_users_email unique (email)
);
create table refresh_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_refresh_tokens_user on refresh_tokens(user_id);
```

- Contraseñas: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (bcrypt por defecto).
- Refresh tokens: guardar **hash** (SHA-256), nunca el valor en claro. Rotación: al refrescar, revocar el anterior y emitir uno nuevo.

## 3. Tokens

- Access token: JWT firmado, vida corta (10–15 min), claims `sub` (user id), `roles`, `iat`, `exp`, `iss`.
- Refresh token: valor opaco aleatorio (32+ bytes, Base64URL), vida larga (7–30 días).
- Firma: HS256 con secreto de ≥ 256 bits desde env (`JWT_SECRET`) para empezar; RS256 con par de claves si habrá varios servicios.
- Emitir con `JwtEncoder` (`NimbusJwtEncoder`), validar con `JwtDecoder` (`NimbusJwtDecoder`).

## 4. Configuración

```kotlin
@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }                       // API stateless con Bearer
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/v1/auth/**", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
              .anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt { } }
        .build()
}
```

- Mapear claim `roles` a authorities con un `JwtAuthenticationConverter` (prefijo `ROLE_`).
- 401 (sin token / token inválido) y 403 (sin permiso) deben responder en el **formato de error del contrato**: configurar `authenticationEntryPoint` y `accessDeniedHandler`.

## 5. Compatibilidad con Ktor `Auth { bearer { } }`

- Ktor solo dispara `refreshTokens` cuando recibe **401** con header `WWW-Authenticate: Bearer`. Asegúrate de que el entry point lo envíe (el de resource server lo hace por defecto; si lo personalizas, mantenlo).
- El endpoint de refresh debe aceptar el refresh token tal como la app lo envía (body JSON o header) y devolver la misma forma que el login.
- Nunca devolver 403 para token expirado (la app no refrescaría).

## 6. Endpoints típicos

| Endpoint | Body | Respuesta |
|---|---|---|
| `POST /api/v1/auth/register` | email, password | 201 + tokens (o 201 sin tokens, según app) |
| `POST /api/v1/auth/login` | email, password | 200 `{accessToken, refreshToken, expiresIn}` |
| `POST /api/v1/auth/refresh` | refreshToken | 200 nuevos tokens / 401 |
| `POST /api/v1/auth/logout` | refreshToken | 204 (revoca) |
| `GET /api/v1/me` | — | 200 usuario actual |

## 7. Buenas prácticas

- Mensaje de error de login genérico ("credenciales inválidas"); no revelar si el email existe.
- Rate limiting en login (p. ej. Bucket4j) si se expone públicamente.
- Validar longitud mínima de contraseña.
- Obtener usuario actual con `@AuthenticationPrincipal jwt: Jwt` → `jwt.subject`.
- Comprobar propiedad de recursos en el servicio (un usuario no accede a pedidos de otro) → 404 o 403 según contrato.

## 8. Verificación (tests)

Login ok/fallido, acceso sin token (401 + `WWW-Authenticate`), token expirado (401), refresh con rotación, refresh revocado (401), rol insuficiente (403).
'@
Write-SkillFile 'springboot-testing/SKILL.md' @'
---
name: springboot-testing
description: Escribe y ejecuta tests para una API Spring Boot con PostgreSQL real vía Testcontainers - tests de integración de endpoints contra el contrato de la app KMP, tests de repositorio y tests unitarios de servicios. Úsala después de implementar o cambiar endpoints, entidades o seguridad.
license: MIT
compatibility: opencode
---

# Testing con Testcontainers + PostgreSQL

Requiere Docker disponible. **No usar H2**: los tests deben correr sobre PostgreSQL real con las mismas migraciones Flyway.

## 1. Configuración compartida de Testcontainers

Initializr con `testcontainers` + `postgresql` genera una clase de configuración. Asegúrate de tener algo como:

```kotlin
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer() = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
}
```

Usa la **misma versión mayor de Postgres** que en `docker-compose.yml`. `@ServiceConnection` configura el datasource solo; Flyway aplica las migraciones al arrancar el contexto.

## 2. Tipos de test

| Tipo | Anotación | Para qué |
|---|---|---|
| Integración de API | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Import(TestcontainersConfiguration::class)` | Endpoint completo: HTTP → DB → JSON |
| Web slice | `@WebMvcTest(XController::class)` + mock del service | Validación, códigos de estado, formato de error |
| Repositorio | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers | Consultas, constraints, mapeo de tipos PG |
| Unitario | JUnit 5 + MockK (Kotlin) / Mockito (Java) | Lógica de servicio |

## 3. Qué probar por cada endpoint del contrato

1. Caso feliz: código de estado y **forma exacta del JSON** (nombres de campos, formato de fechas, enums) tal como la app KMP lo deserializa.
2. Validación: body inválido → 400 con el formato de error del contrato.
3. No encontrado → 404.
4. Conflicto (unique) → 409.
5. Seguridad (si aplica): sin token → 401; otro usuario → 403/404.
6. Paginación: `page`, `size`, límites, orden.

Comprueba el JSON con `jsonPath` (MockMvc) o comparando contra un fixture:

```kotlin
mockMvc.get("/api/v1/orders/$id") { header("Authorization", "Bearer $token") }
    .andExpect {
        status { isOk() }
        jsonPath("$.id") { value(id.toString()) }
        jsonPath("$.status") { value("PENDING") }
        jsonPath("$.createdAt") { isString() }
    }
```

## 4. Test de contrato con los DTOs de la app (recomendado)

Si el módulo `shared` de la app publica sus DTOs (o puedes copiarlos a `src/test`), añade un test que deserialice la respuesta real del backend con **kotlinx.serialization y la misma config `Json {}` de la app**. Es la forma más fiable de garantizar compatibilidad.

```kotlin
val json = Json { ignoreUnknownKeys = false /* igual que la app */ }
val body = restClient.get().uri("/api/v1/orders/$id").retrieve().body(String::class.java)!!
json.decodeFromString<OrderDto>(body) // falla si falta o sobra un campo
```

## 5. Datos de prueba

- Cada test crea sus datos (builders/fixtures) y no depende del orden.
- Limpieza: `@Transactional` en tests de repositorio; en tests de integración HTTP, `TRUNCATE ... CASCADE` en `@BeforeEach` o `@Sql`.
- Reutilizar contenedor entre clases (configuración compartida) para velocidad.

## 6. Ejecutar

```bash
cd backend && ./gradlew test
```

Revisa `build/reports/tests/test/index.html` si algo falla. No marcar el paso como terminado con tests rojos o deshabilitados.
'@
Write-Host "Skills instaladas en: $dest  (archivos escritos: $($script:installed), omitidos: $($script:skipped))"
Get-ChildItem -LiteralPath $dest -Directory | ForEach-Object { Write-Host $_.Name }
Write-Host ""
Write-Host "Siguiente paso: abre opencode en el proyecto y escribe:"
Write-Host '  "Usa la skill kmp-backend-orchestrator para crear la API Spring Boot y la base PostgreSQL de esta app"'
