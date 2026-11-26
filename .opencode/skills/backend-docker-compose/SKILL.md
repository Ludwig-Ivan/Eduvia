---
name: backend-docker-compose
description: Crea el entorno local y la imagen de despliegue del backend - Dockerfile multi-stage para Spring Boot, docker-compose con PostgreSQL y la API, variables de entorno, healthchecks y cómo conectar el emulador Android/simulador iOS al backend local. Úsala para levantar o contenerizar el backend.
license: MIT
compatibility: opencode
---

# Docker y entorno local

## 1. `backend/compose.yaml` (solo Postgres, para desarrollo con `bootRun`)

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      retries: 10
volumes:
  pgdata:
```

Con la dependencia `spring-boot-docker-compose`, `./gradlew bootRun` levanta este compose automáticamente y configura el datasource.

## 2. `backend/Dockerfile` (multi-stage)

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=build /src/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
```

Ajusta la versión de Java a la del `toolchain` del proyecto. Añade `.dockerignore` (`build/`, `.gradle/`, `*.iml`, `.env`).

## 3. `docker-compose.yml` en la raíz (stack completo)

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-app}
      POSTGRES_USER: ${DB_USER:-app}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-app}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-app} -d ${DB_NAME:-app}"]
      interval: 5s
      retries: 10
  api:
    build: ./backend
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-app}
      DB_USER: ${DB_USER:-app}
      DB_PASSWORD: ${DB_PASSWORD:-app}
      JWT_SECRET: ${JWT_SECRET:?define JWT_SECRET en .env}   # quitar si no hay auth
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DOCKER_COMPOSE_ENABLED: "false"
    ports: ["8080:8080"]
volumes:
  pgdata:
```

Crea `.env.example` con las variables (sin secretos reales) y añade `.env` al `.gitignore`.

## 4. Conectar la app KMP al backend local

| Plataforma | Host |
|---|---|
| Emulador Android | `http://10.0.2.2:8080` |
| Simulador iOS | `http://localhost:8080` |
| Dispositivo físico | IP LAN del equipo, p. ej. `http://192.168.1.50:8080` |

- Android con HTTP en claro requiere `network_security_config` permitiendo ese host (solo en debug).
- iOS requiere excepción ATS en `Info.plist` para HTTP en desarrollo.
- No cambies la configuración de la app sin confirmación (eso es del paso `kmp-client-sync`).

## 5. Verificación

```bash
docker compose up -d --build
curl -fsS http://localhost:8080/actuator/health   # {"status":"UP"}
docker compose logs api | grep -i flyway           # migraciones aplicadas
```
