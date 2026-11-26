# ADR 0006 — Errores con `ProblemDetail` (RFC 9457)

## Estado

Aceptada.

## Contexto

El cliente Ktor de la app parsea errores como `ProblemDto` con `status`, `title`, `detail`, `instance` y una lista `errors` de `{field, message}`. El contrato recoge ese formato en [`docs/backend/api-contract.md`](../backend/api-contract.md) y el fixture [`problem-validation.json`](../../contract/fixtures/problem-validation.json) contiene un 400 con dos errores de campo.

Con Spring Boot 4, el manejador genérico del framework intercepta antes los fallos de `@Valid`, los cuerpos JSON ilegibles y los tipos inválidos de path/query. Sin una precedencia explícita, esos casos devuelven el formato de error de Boot y se pierde la propiedad `errors` que espera la app.

## Decisión

Centralizar todos los errores de la API en [`GlobalExceptionHandler.kt`](../../backend/src/main/kotlin/com/example/librerias/api/web/GlobalExceptionHandler.kt) con `@RestControllerAdvice` y **`@Order(Ordered.HIGHEST_PRECEDENCE)`**. Cada excepción se convierte en `ProblemDetail` con `Content-Type: application/problem+json`:

- `ApiException` (400/401/404/409) conserva el estado decidido por el servicio y escribe `properties.errors`.
- `@Valid` → 400 con `errors` por campo.
- Cuerpo no legible o tipo inválido → 400 con `errors` explicativo, sin filtrar detalles internos.
- Excepción no prevista → 500 con detalle genérico y traza completa solo en logs.
- La capa de seguridad añade `WWW-Authenticate: Bearer` en el 401, para que el plugin `Auth` de Ktor renueve el token y reintente (decisión 3: JWT completo).

`application.yml` activa `spring.mvc.problemdetails.enabled: true` y `server.error.include-stacktrace: never`; Swagger declara las respuestas con `@ApiResponse`.

El porqué: un único formato de error hace que la app pueda traducir cualquier fallo a `AppResult.Error(message)` y resaltar campos concretos sin ramas especiales por tipo de excepción; la precedencia máxima es la única forma de garantizarlo también para los errores que Spring gestiona por defecto.

## Consecuencias

### Positivas

- Un 404 real (`GET /groups/999999`) devuelve `{"status":404,"title":"Not Found","detail":"Grupo no encontrado","errors":[]}` con el content type esperado; un 401 sin token incluye el desafío Bearer. Hay evidencia en [`docs/backend/compat-report.md`](../backend/compat-report.md).
- Los errores de validación `acceptedTerms = false` llegan con `errors` y el fixture estricto de la app los deserializa sin campos desconocidos.
- No se exponen trazas ni mensajes de deserialización al cliente; el diagnóstico queda en los logs del backend.
- La documentación OpenAPI describe códigos 400/401/404/409 en los controllers con `@ApiResponse`, coherente con lo que produce el advice.

### Negativas

- El advice añade una capa de traducción que hay que mantener cuando aparezca un nuevo tipo de excepción; si no se añade un handler, cae en el 500 genérico.
- `@Order(HIGHEST_PRECEDENCE)` es sensible a cambios de versión de Spring Boot: al actualizar a Boot 4 hubo que corregirlo tras detectar que los `@Valid` devolvían el formato del framework (ver `PROGRESS.md`, Fase 3).
- springdoc muestra `200` de forma genérica en `/v3/api-docs` para algunos endpoints; los códigos reales están verificados por tests E2E y peticiones reales, no por la UI (limitación 3 de `compat-report.md`).

## Alternativas consideradas

- **Devolver un `String` o un DTO de error propio:** descartada; rompería la interoperabilidad con `ProblemDto` y con `application/problem+json`.
- **Dejar el manejador por defecto de Spring Boot:** descartada; pierde `errors` y no garantiza el mismo cuerpo para validación, tipos inválidos y errores de dominio.
- **Manejar errores en cada controller:** descartada; duplicaría lógica en los 26 paths y facilitaría respuestas inconsistentes.
- **Exponer la excepción y su stacktrace al cliente:** descartada por seguridad y porque la app solo necesita `detail` y `errors`.

## Enlaces

- Decisión de autenticación: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), fila 3.
- Código del backend: [`GlobalExceptionHandler.kt`](../../backend/src/main/kotlin/com/example/librerias/api/web/GlobalExceptionHandler.kt), [`SecurityConfig.kt`](../../backend/src/main/kotlin/com/example/librerias/api/config/SecurityConfig.kt), [`application.yml`](../../backend/src/main/resources/application.yml), [`Dtos.kt`](../../backend/src/main/kotlin/com/example/librerias/api/web/dto/Dtos.kt).
- Código de la app: [`ApiClient.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiClient.kt), [`ApiDtos.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiDtos.kt).
- Evidencia: [`contract/fixtures/problem-validation.json`](../../contract/fixtures/problem-validation.json), [`ContractFixturesFileTest.kt`](../../Librerias_eduvia/shared/src/androidHostTest/kotlin/com/example/librerias/contract/ContractFixturesFileTest.kt), [`docs/backend/compat-report.md`](../backend/compat-report.md).
