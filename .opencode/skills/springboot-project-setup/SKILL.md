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
