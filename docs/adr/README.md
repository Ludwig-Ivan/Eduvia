# Decisiones arquitectónicas (ADR) — EduvIA

Índice de las decisiones ya aplicadas. Todas están en estado **Aceptada** y se apoyan en las decisiones confirmadas por el usuario en [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md) y en la evidencia de [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).

| ADR | Decisión | Por qué se tomó |
|---|---|---|
| [0001 — Backend Kotlin con Spring Boot en `backend/`](./0001-backend-kotlin-spring-boot.md) | Kotlin/Spring Boot 4.2.0 en su carpeta, JWT completo y Compose `miapp` | La app ya tiene login y el contrato lo define ella; el lenguaje coincide con KMP |
| [0002 — Build de Gradle separado](./0002-build-gradle-separado.md) | `backend/settings.gradle.kts` y `gradlew.bat` propios | Mezclar Android/KMP con Boot en un mismo build da conflictos y sincronización lenta |
| [0003 — Flyway como fuente del esquema](./0003-flyway-como-fuente-del-esquema.md) | V1 → V2 → V1000 solo en `dev`, Hibernate en `validate` | Esquema reproducible y alineado con los IDs `Long`, `NULL↔0` y el offset 1000 |
| [0004 — Testcontainers con PostgreSQL real](./0004-testcontainers-postgres-real.md) | `postgres:17-alpine`, E2E etiquetados y JaCoCo ≥ 80 % | El SQL, las restricciones y el JSON deben probarse contra lo que realmente se despliega |
| [0005 — Fixtures de contrato compartidos](./0005-fixtures-de-contrato-compartidos.md) | 11 JSON en `contract/fixtures/`, test estricto `ignoreUnknownKeys=false` | Detectar cambios de forma entre backend y app en ambos sentidos |
| [0006 — `ProblemDetail` (RFC 9457)](./0006-problemdetail-rfc-9457.md) | `@RestControllerAdvice` con `@Order(HIGHEST_PRECEDENCE)` y `properties.errors` | El cliente Ktor necesita un único formato de error, también en validaciones del framework |
| [0007 — Base URL Android `10.0.2.2`](./0007-base-url-android-10-0-2-2.md) | Android `10.0.2.2`, iOS `localhost`, cleartext restringido | El emulador alcanza al host por esa IP; `localhost` dentro del emulador no sirve |

## Documentación relacionada

- Contrato y modelo: [`docs/backend/api-contract.md`](../backend/api-contract.md), [`docs/backend/openapi.yaml`](../backend/openapi.yaml), [`docs/backend/data-model.md`](../backend/data-model.md).
- Compatibilidad y ejecución: [`docs/backend/compat-report.md`](../backend/compat-report.md), [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md), [`docs/backend/test-plan.md`](../backend/test-plan.md).
- Dudas abiertas: [`docs/backend/OPEN-QUESTIONS.md`](../backend/OPEN-QUESTIONS.md). Las marcadas `⚠️ Por confirmar` siguen pendientes de decisión.
- Hallazgos H1–H13: [`docs/HALLAZGOS.md`](../HALLAZGOS.md). Se documentan, no se arreglan en esta documentación (salvo H6 y H12, resueltos por ser cambios de `.md`).
- Guía de trabajo original: [`GUIA-OPENCODE.md`](../../GUIA-OPENCODE.md).
