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
