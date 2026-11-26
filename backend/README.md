# Backend de EduvIA

API REST para la app Kotlin Multiplatform: **Spring Boot 4.2.0 + Kotlin + Java 21**, PostgreSQL 17 con Flyway, autenticación JWT completa y Swagger UI. Todo el código vive bajo `com.example.librerias.api` y responde en `/api/v1`.

## Comandos rápidos

Ejecutar siempre desde esta carpeta (`backend/`), en Windows:

```powershell
gradlew.bat build                 # compila, prueba y genera el jar
gradlew.bat bootRun               # arranca en http://localhost:8080
gradlew.bat check                 # pruebas + contrato + JaCoCo (≥ 80 %)
gradlew.bat e2eTest               # pruebas E2E contra un stack levantado
```

Para `bootRun`, PostgreSQL puede levantarse solo con `docker compose up -d postgres` desde la raíz (o con `backend/compose.yaml`, integrado con Spring Boot). El stack completo se levanta con `docker compose up -d --build --wait` desde la raíz; `scripts/test-all.ps1` ejecuta backend, app, stack aislado y E2E de una vez.

## Perfiles

| Perfil | Qué hace |
|---|---|
| `dev` (el de Compose por defecto) | Aplica `db/migration` **y** `db/dev-data/V1000__seed.sql` con datos demo. Adminer es opcional: `docker compose --profile tools up -d`. |
| `test` | Solo `db/migration`, sin semilla; usa PostgreSQL real vía Testcontainers y datos creados por cada prueba. |
| Sin perfil `dev` (producción) | Solo migraciones versionadas; hay que inyectar `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` desde el entorno. |

Hibernate está en `ddl-auto: validate`: Flyway es quien crea el esquema.

## Swagger y health

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Documentación OpenAPI: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

Los controladores documentan rutas y respuestas con `@Tag`, `@Operation`, `@ApiResponse` y `@Schema`.

## Estado verificado

38 tests de backend, 19 de la app KMP y 4 E2E en verde; cobertura JaCoCo del 86,2 %. El contrato runtime coincide con el diseño (`missing=[]`, `extra=[]`) y `compat-report.md` no tiene incompatibilidades.

⚠️ Por confirmar: `spring.application.name=api` está duplicado en `src/main/resources/application.properties` y `application.yml` (H11). Ver [`../docs/backend/OPEN-QUESTIONS.md`](../docs/backend/OPEN-QUESTIONS.md) Q13 y [`../docs/HALLAZGOS.md`](../docs/HALLAZGOS.md) H11.

## Documentación

- [Decisiones](./../docs/backend/DECISIONS.md) · [Contrato de API](./../docs/backend/api-contract.md) · [OpenAPI](./../docs/backend/openapi.yaml)
- [Modelo de datos](./../docs/backend/data-model.md) · [Plan de pruebas](./../docs/backend/test-plan.md)
- [Progreso por fases](./../docs/backend/PROGRESS.md) · [Compatibilidad app ↔ API](./../docs/backend/compat-report.md)
- [Preguntas abiertas](./../docs/backend/OPEN-QUESTIONS.md) · [ADR](../docs/adr/README.md)
