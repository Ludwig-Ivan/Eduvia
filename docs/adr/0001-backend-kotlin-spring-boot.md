# ADR 0001 — Backend en Kotlin con Spring Boot dentro de `backend/`

## Estado

Aceptada.

## Contexto

El repositorio ya contiene la app Kotlin Multiplatform en `Librerias_eduvia/`, con modelos de dominio, repositorios y pantallas, pero sin servidor propio. La guía de trabajo (`GUIA-OPENCODE.md`) pide una API REST con PostgreSQL y pruebas automáticas; el usuario confirmó las decisiones 1 a 6 de [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md): backend en `backend/`, lenguaje Kotlin, autenticación JWT completa, paquete `com.example.librerias.api`, nombre de Compose `miapp` y Adminer en el perfil opcional `tools`.

La app tiene login y registro, por lo que se aplicó la Fase 3b de la guía: no basta con una API abierta. Además, el contrato lo define la app: IDs `Long`, JSON `camelCase` y fechas `YYYY-MM-DD`, según [`docs/backend/api-contract.md`](../backend/api-contract.md).

## Decisión

Crear un servicio REST independiente en `backend/`, escrito en Kotlin 2.4.21 sobre Spring Boot **4.2.0** y Java 21, con paquete base `com.example.librerias.api` (derivado del namespace `com.example.librerias` de la app). El stack se levanta con el proyecto de Compose `miapp` (PostgreSQL 17 + API + Adminer opcional).

La autenticación es JWT completa: `login`, `registro`, `refresh` y `logout`, con header `Bearer`, secreto `JWT_SECRET`, access token de **900 s** y refresh token de **7 días**, con rotación. El porqué: la app ya tiene flujo de sesión, y el plugin `Auth` de Ktor necesita un 401 con `WWW-Authenticate: Bearer` para renovar solo; un token de acceso corto limita el impacto de un robo sin obligar a iniciar sesión cada pocos minutos.

La API se adapta al contrato de la app en lugar de imponer convenciones nuevas: entidades JPA convertidas siempre a DTOs espejo, paginación `PageResponse<T>` propia y documentación Swagger con `@Tag`, `@Operation`, `@ApiResponse` y `@Schema`.

## Consecuencias

### Positivas

- Kotlin es el mismo lenguaje que la app, así que los DTOs, enums y mappers se pueden comparar campo por campo con [`docs/backend/compat-report.md`](../backend/compat-report.md).
- Spring Boot aporta JPA, Flyway, Security, Validation, Actuator y OpenAPI sin inventar infraestructura propia; la versión 4.2.0 está fijada en [`backend/build.gradle.kts`](../../backend/build.gradle.kts).
- El JWT full cubre registro, login, renovación y revocación, y los tiempos quedan centralizados en [`backend/src/main/resources/application.yml`](../../backend/src/main/resources/application.yml).
- El paquete `com.example.librerias.api` hace evidente que el backend es el proveedor del contrato que consume `com.example.librerias`.
- Adminer queda detrás del perfil `tools` en [`docker-compose.yml`](../../docker-compose.yml), de modo que no se expone en un arranque normal.

### Negativas

- Hay que mantener un build de backend además del build de la app; ver el ADR 0002.
- El equipo debe inyectar `JWT_SECRET` fuera del desarrollo local; el valor por defecto de `application.yml` es solo para pruebas en la máquina.
- ⚠️ Por confirmar: `spring.application.name=api` aparece duplicado en `application.properties` y `application.yml` (H11, sin corregir). ¿Unificar la configuración? Ver [`docs/backend/OPEN-QUESTIONS.md`](../backend/OPEN-QUESTIONS.md) Q13 y [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H11.

## Alternativas consideradas

- **Java en el backend:** descartada porque la app es Kotlin y el usuario confirmó Kotlin; además obligaría a traducir modelos y pruebas.
- **Node.js, Django u otro framework:** descartados porque la guía y el entorno (JDK 21, Gradle, Testcontainers) ya apuntan a Spring Boot.
- **Meter la API dentro del módulo `shared` de KMP:** descartada; mezcla targets móviles con un servidor JVM.
- **Solo access token, sin refresh/logout:** descartada porque la app tiene sesión y el contrato incluye `expiresIn` y renovación.
- **Publicar Adminer siempre:** descartada por seguridad; se prefiere el perfil `tools`.

## Enlaces

- Decisión original: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), filas 1–6.
- Contrato derivado de la app: [`docs/backend/api-contract.md`](../backend/api-contract.md).
- Configuración y dependencias: [`backend/build.gradle.kts`](../../backend/build.gradle.kts), [`backend/src/main/resources/application.yml`](../../backend/src/main/resources/application.yml).
- Código: [`backend/src/main/kotlin/com/example/librerias/api/ApiApplication.kt`](../../backend/src/main/kotlin/com/example/librerias/api/ApiApplication.kt), [`JwtConfig.kt`](../../backend/src/main/kotlin/com/example/librerias/api/config/JwtConfig.kt), [`SecurityConfig.kt`](../../backend/src/main/kotlin/com/example/librerias/api/config/SecurityConfig.kt), [`AuthService.kt`](../../backend/src/main/kotlin/com/example/librerias/api/service/AuthService.kt).
- Despliegue local: [`docker-compose.yml`](../../docker-compose.yml), [`.env.example`](../../.env.example), [`backend/Dockerfile`](../../backend/Dockerfile).
- Ejecución real por fases: [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).
