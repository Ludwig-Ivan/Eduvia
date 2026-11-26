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
