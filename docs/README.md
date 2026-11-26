# Documentación — EduvIA

Índice navegable del repositorio. Las rutas existentes están enlazadas; los documentos que aún no existen se citan con **`por crear`** y serán escritos por otro agente. Las fuentes originales se reutilizan mediante enlaces, no se duplican aquí. Para el inventario completo de código y documentación, ver [`INVENTARIO.md`](INVENTARIO.md).

## Inicio

- [README.md (raíz)](../README.md) — qué es EduvIA, requisitos, inicio rápido en Windows y credenciales de la demo.
- [Este índice](README.md) — mapa de toda la documentación agrupada por tema.
- [INVENTARIO.md](INVENTARIO.md) — inventario de módulos, clases, endpoints, tablas, pruebas y entregables documentados.

## Arquitectura

- [arquitectura.md](arquitectura.md) — capas de la app y del backend, flujo punta a punta, errores, paginación, JWT, responsabilidades y reglas de oro.

## App

- [app-kmp.md](app-kmp.md) — módulos, source sets, inyección de dependencias, red por plataforma y estados de la UI.
- [guias/app-kotlin.md](guias/app-kotlin.md) — explicación para estudiantes de la app KMP y del código compartido.

## Backend

- [backend.md](backend.md) — paquetes, capas, errores, paginación, validación, seguridad y perfiles.
- [backend/PROGRESS.md](backend/PROGRESS.md) — evidencia real de las fases 1–7 y resultado de pruebas.
- [backend/DECISIONS.md](backend/DECISIONS.md) — decisiones confirmadas 1–13 del backend y la app.

## API

- [api.md](api.md) — tabla de endpoints con ejemplos reales y acceso a Swagger UI.
- [backend/api-contract.md](backend/api-contract.md) — convenciones, operaciones A1–A9, G1–G7, T1–T6, M1–M9 y C1–C3, y reglas de negocio.
- [backend/openapi.yaml](backend/openapi.yaml) — contrato OpenAPI 3.0.3, 26 paths / 34 operaciones / 36 schemas.
- [backend/compat-report.md](backend/compat-report.md) — comparación app KMP ↔ API: DTOs, enums, fechas, errores y códigos de estado.
- [contract/fixtures/](../contract/fixtures/) — 11 respuestas JSON reales compartidas por backend y app.

## Base de datos

- [base-de-datos.md](base-de-datos.md) — ER, tablas, índices, migraciones y datos de ejemplo.
- [backend/data-model.md](backend/data-model.md) — modelo entidad-relación, 10 tablas, mapeo DTO ↔ tabla e índices.
- [V1__init_schema.sql](../backend/src/main/resources/db/migration/V1__init_schema.sql) — esquema inicial y constraints.
- [V2__comments.sql](../backend/src/main/resources/db/migration/V2__comments.sql) — comentarios SQL sobre el esquema.
- [V1000__seed.sql](../backend/src/main/resources/db/dev-data/V1000__seed.sql) — datos demo, solo perfil `dev`.

## Docker

- [docker.md](docker.md) — servicios, puertos, volúmenes, variables, healthchecks y perfiles.
- [docker-compose.yml](../docker-compose.yml) — proyecto `miapp`: `postgres`, `api` y `adminer` (perfil `tools`), volumen `pgdata`.
- [.env.example](../.env.example) — plantilla de `DB_*`, puertos, perfil y `JWT_SECRET`.
- [backend/Dockerfile](../backend/Dockerfile) — imagen multi-stage JDK 21 → JRE 21, usuario no root y healthcheck.

## Pruebas

- [pruebas.md](pruebas.md) — pirámide de pruebas, suites, comandos, fixtures de contrato y cobertura.
- [backend/test-plan.md](backend/test-plan.md) — escenarios previstos por endpoint y su estado.
- [scripts/test-all.ps1](../scripts/test-all.ps1) — verificación completa en 5 pasos con limpieza en `finally`.
- [backend/PROGRESS.md](backend/PROGRESS.md) — resultados ejecutados: 38 tests backend, 19 KMP, 4 E2E, JaCoCo 86,2 %.

## Desarrollo

- [guia-de-desarrollo.md](guia-de-desarrollo.md) — cómo añadir un endpoint de punta a punta y convenciones de código.
- [GUIA-OPENCODE.md](../GUIA-OPENCODE.md) — plan original por fases, puertas de calidad y solución de problemas. Tiene nombres de plantilla (H7).

## Operación

- [operacion.md](operacion.md) — runbook de comandos, logs, reinicio, reset de datos y problemas comunes.
- [HALLAZGOS.md](HALLAZGOS.md) — bugs, inconsistencias y código confuso H1–H13; documentados sin arreglarlos, salvo H6 y H12.
- [backend/OPEN-QUESTIONS.md](backend/OPEN-QUESTIONS.md) — dudas Q1–Q13 con decisión provisional; las no confirmadas llevan `⚠️ Por confirmar`.

## ADR

- [adr/README.md](adr/README.md) — índice de decisiones de arquitectura (0001–0007); cada ADR también se enlaza desde [arquitectura.md](arquitectura.md).

## Glosario

- [glosario.md](glosario.md) — términos del dominio (junta, grupo, alumno, tutor, entrega, asistencia, recordatorio) y técnicos (KMP, Ktor, DTO, Flyway, JWT, `ProblemDetail`, `PageResponse`, offset 1000, `parentId` centinela, entre otros).

## Guías juveniles

- [guias/README.md](guias/README.md) — índice de las tres guías explicadas como un restaurante escolar.
- [guias/app-kotlin.md](guias/app-kotlin.md) — de dónde sale cada pantalla y qué ocurre al pulsar «Publicar junta».
- [guias/api-rest.md](guias/api-rest.md) — qué es una API, por qué hay un token y qué significan GET, POST, PUT y DELETE.
- [guias/base-de-datos.md](guias/base-de-datos.md) — tablas, claves, migraciones y Flyway en lenguaje sencillo.

## Documentación de backend (detalle)

- [backend/api-contract.md](backend/api-contract.md) — contrato que debe cumplir la API frente a la app.
- [backend/openapi.yaml](backend/openapi.yaml) — especificación OpenAPI validada contra la API en ejecución.
- [backend/data-model.md](backend/data-model.md) — esquema y decisiones de persistencia.
- [backend/DECISIONS.md](backend/DECISIONS.md) — origen y valor de cada decisión confirmada.
- [backend/PROGRESS.md](backend/PROGRESS.md) — historial de fases y verificación final.
- [backend/compat-report.md](backend/compat-report.md) — compatibilidad app ↔ API y limitaciones documentadas.
- [backend/OPEN-QUESTIONS.md](backend/OPEN-QUESTIONS.md) — preguntas abiertas y decisiones provisionales.
- [backend/test-plan.md](backend/test-plan.md) — plan de escenarios por endpoint.

> Los puntos sin confirmar se marcan `⚠️ Por confirmar` y se enlazan a [backend/OPEN-QUESTIONS.md](backend/OPEN-QUESTIONS.md). Los bugs y confusiones se enlazan a [HALLAZGOS.md](HALLAZGOS.md).
