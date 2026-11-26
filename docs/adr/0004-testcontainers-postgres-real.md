# ADR 0004 — Pruebas de integración con PostgreSQL real vía Testcontainers

## Estado

Aceptada.

## Contexto

La guía exige que ninguna fase se dé por terminada sin pruebas automáticas en verde y prohíbe desactivar pruebas para que el build pase. El backend ejecuta SQL propio (`returning id`, `date::text`, `truncate ... restart identity cascade`), migraciones Flyway y restricciones `unique`/FK/`check`; una base en memoria no se comporta igual que PostgreSQL 17. Además, el contrato incluye serialización Boot 4 (Jackson 3), paginación y errores `ProblemDetail`, que deben verificarse con peticiones HTTP reales.

## Decisión

Usar **Testcontainers con `postgres:17-alpine`** para las pruebas de integración, inyectado con `@ServiceConnection` desde [`TestcontainersConfiguration.kt`](../../backend/src/test/kotlin/com/example/librerias/api/TestcontainersConfiguration.kt). El perfil `test` aplica solo `classpath:db/migration` (sin `V1000__seed.sql`) y mantiene `ddl-auto: validate`.

Cada prueba:
- arranca el contexto Spring en puerto aleatorio con `TestRestTemplate` de Boot 4;
- limpia las tablas con `truncate ... restart identity cascade` en `@BeforeEach` y reinicia la secuencia de `calendar_events` para aislar los IDs;
- usa `TestData` con email/teléfono únicos por prueba;
- llama a la API por HTTP y comprueba también lo persistido con `JdbcTemplate`.

Las pruebas E2E se separan con `@Tag("e2e")` y la tarea `e2eTest`: se ejecutan contra el stack real de Docker, no contra el contenedor de test. JaCoCo exige cobertura de líneas ≥ 80 % y `check` depende de esa verificación. Resultado final registrado: **38 tests de backend, 19 de KMP y 4 E2E en verde, 86,2 % de cobertura**.

El porqué: la base de datos, el SQL y la forma del JSON son parte del contrato; solo un PostgreSQL real y una API en ejecución pueden detectar una FK rota, un `unique` mal puesto o un campo que la app no sabe deserializar.

## Consecuencias

### Positivas

- Las mismas migraciones Flyway se validan en el contenedor y en Compose; no hay una «BD de test» con esquema distinto.
- Los tests cubren los 26 paths del contrato y las reglas de negocio (IDs `1000 + meetings.id`, `parentId` `NULL↔0`, 409 por duplicados); ver [`docs/backend/test-plan.md`](../backend/test-plan.md) y [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).
- La separación `test`/`e2eTest` permite correr la suite rápida sin Docker Compose y reservar el stack para verificar imagen, healthchecks, red y persistencia tras reinicio.
- El informe [`docs/backend/compat-report.md`](../backend/compat-report.md) queda sin incompatibilidades abiertas, con `missing=[]` y `extra=[]` al comparar `/v3/api-docs` con el contrato.

### Negativas

- Docker Desktop es obligatorio incluso para las pruebas unitarias del backend que usan el contexto de Spring; sin daemon falla Testcontainers.
- La limpieza con `truncate` y el arranque de contenedores hacen la suite más lenta que un H2 en memoria.
- Los cambios de Boot 4 obligaron a ajustar imports (`tools.jackson.*`, `org.springframework.boot.resttestclient.*`) en lugar de silenciar los fallos; están documentados en [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).
- Hallazgo documentado y sin corregir: algunas casillas de `test-plan.md` siguen pendientes aunque la ejecución final está en verde (H5). Ver [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H5.

## Alternativas consideradas

- **H2 en memoria:** descartada; no interpreta igual el SQL de PostgreSQL ni las migraciones, y ocultaría errores de restricciones.
- **Solo mocks de repositorio:** descartada; no cubre FK, `unique`, transacciones ni serialización HTTP.
- **Solo E2E contra Compose:** descartada; es lenta y no da aislamiento por prueba.
- **Bajar el umbral de cobertura o desactivar pruebas:** descartado por la regla central de la guía; el umbral sigue en 80 %.

## Enlaces

- Decisiones y progreso: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).
- Configuración: [`backend/build.gradle.kts`](../../backend/build.gradle.kts), [`backend/src/test/resources/application-test.yml`](../../backend/src/test/resources/application-test.yml).
- Código de prueba: [`TestcontainersConfiguration.kt`](../../backend/src/test/kotlin/com/example/librerias/api/TestcontainersConfiguration.kt), [`IntegrationTestBase.kt`](../../backend/src/test/kotlin/com/example/librerias/api/IntegrationTestBase.kt), [`TestData.kt`](../../backend/src/test/kotlin/com/example/librerias/api/TestData.kt), [`E2eIntegrationTest.kt`](../../backend/src/test/kotlin/com/example/librerias/api/E2eIntegrationTest.kt).
- Ejecución única: [`scripts/test-all.ps1`](../../scripts/test-all.ps1).
- Hallazgo: [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H5.
