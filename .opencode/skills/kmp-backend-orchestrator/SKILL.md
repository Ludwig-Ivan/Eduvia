---
name: kmp-backend-orchestrator
description: Punto de entrada para generar un backend REST en Spring Boot con base de datos PostgreSQL a partir de una app móvil Kotlin Multiplatform (KMP) existente. Úsala cuando el usuario pida "crear el backend/API para la app", "generar la API desde la app KMP" o similar. Coordina el orden de las demás skills.
license: MIT
compatibility: opencode
---

# Orquestador: app KMP → API Spring Boot + PostgreSQL

Esta skill define el flujo completo. Cada paso tiene su propia skill: cárgala con la herramienta `skill` antes de ejecutarlo.

## Antes de empezar: decisiones (preguntar al usuario si no están claras)

1. **Ubicación del backend**: carpeta `backend/` dentro del mismo repo (por defecto) o repo separado.
2. **Lenguaje del backend**: Kotlin (por defecto, coherente con KMP) o Java.
3. **Autenticación**: ¿la app envía tokens (Bearer/JWT), API keys, o nada? Detectarlo en el paso 1 y confirmar.
4. **Paquete base**: p. ej. `com.empresa.app.api` (derivar del `applicationId`/namespace de la app si existe).

Si el usuario no está disponible, usa los valores por defecto y anótalos en `docs/backend/DECISIONS.md`.

## Flujo (en este orden)

| # | Skill | Entregable |
|---|-------|-----------|
| 1 | `kmp-api-contract` | `docs/backend/api-contract.md` + `docs/backend/openapi.yaml` |
| 2 | `postgres-schema-design` | `docs/backend/data-model.md` + migraciones Flyway |
| 3 | `springboot-project-setup` | Proyecto `backend/` compilando |
| 4 | `springboot-jpa-postgres` | Entidades, repositorios, migraciones aplicadas |
| 5 | `springboot-rest-api` | Controllers, DTOs, validación, errores |
| 6 | `springboot-security-jwt` | Solo si la app usa autenticación |
| 7 | `springboot-testing` | Tests de integración con Testcontainers en verde |
| 8 | `backend-docker-compose` | `docker compose up` levanta Postgres + API |
| 9 | `kmp-client-sync` | Informe de compatibilidad app ↔ API (y cambios opcionales en la app) |

## Reglas generales

- **El contrato manda.** La fuente de verdad es lo que la app KMP ya envía y espera recibir. El backend se adapta a la app, no al revés, salvo que el usuario lo autorice.
- **No modificar la app KMP** hasta el paso 9, y solo con confirmación explícita.
- Después de cada paso: compilar (`./gradlew build -x test` en `backend/`) y, desde el paso 7, correr tests. No avanzar con el build roto.
- No inventar versiones de librerías: usar las que da start.spring.io o las verificadas en Maven Central.
- **Windows**: los ejemplos de comandos están en sintaxis bash. En Windows usa `gradlew.bat` en lugar de `./gradlew`, PowerShell para scripts (p. ej. `scan-kmp.ps1`) y `curl.exe` en vez del alias `curl` de PowerShell. Docker Desktop debe estar corriendo para Testcontainers y docker compose.
- Mantener un registro breve en `docs/backend/PROGRESS.md` (paso, estado, pendientes) para poder retomar.
- Si algo del contrato es ambiguo (campo sin uso claro, endpoint sin respuesta visible), anotarlo en `docs/backend/OPEN-QUESTIONS.md` en vez de suponer en silencio.

## Criterio de terminado

- `docker compose up` levanta todo; `GET /actuator/health` responde `UP`.
- Todos los endpoints del contrato existen y tienen al menos un test de integración.
- Swagger UI en `/swagger-ui.html` coincide con `docs/backend/openapi.yaml`.
- El informe de `kmp-client-sync` no tiene incompatibilidades abiertas.
