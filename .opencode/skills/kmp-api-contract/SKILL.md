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
