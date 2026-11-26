---
name: kmp-client-sync
description: Verifica y sincroniza la app Kotlin Multiplatform con la API Spring Boot generada - compara rutas, DTOs, nulabilidad, enums, fechas y errores, produce un informe de compatibilidad y, con confirmación, ajusta base URL, DTOs o configuración de red de la app. Úsala al terminar o cambiar el backend.
license: MIT
compatibility: opencode
---

# Sincronizar app KMP ↔ API

## 1. Fuentes a comparar

- App: llamadas Ktor/Ktorfit y DTOs `@Serializable` (re-escanear con `.opencode/skills/kmp-api-contract/scripts/scan-kmp.sh`, o `scan-kmp.ps1` en Windows).
- Backend: OpenAPI real generado por springdoc (`curl http://localhost:8080/v3/api-docs.yaml`) — no solo el `openapi.yaml` de diseño.

## 2. Checklist por endpoint

- [ ] Método y ruta idénticos (incluido prefijo `/api/v1` y barras finales).
- [ ] Path y query params con el mismo nombre y tipo.
- [ ] Request: todos los campos que envía la app los acepta el backend con el mismo nombre.
- [ ] Response: todo campo **no nulo sin default** en el DTO de la app siempre viene en la respuesta.
- [ ] Sin campos extra si la app tiene `ignoreUnknownKeys = false`.
- [ ] Enums: mismos literales.
- [ ] Fechas: formato que la app parsea (`Instant` → ISO-8601 con `Z` u offset).
- [ ] Números: `Long` vs `Int` (ids grandes), decimales como número o string según la app.
- [ ] Códigos de estado que la app maneja (`201`, `204`, `404`, `409`...) y formato de error.
- [ ] Auth: 401 + `WWW-Authenticate: Bearer` para que el refresh de Ktor funcione.

## 3. Informe `docs/backend/compat-report.md`

| Endpoint | Estado | Problema | Lado a corregir | Propuesta |
|---|---|---|---|---|
| `GET /api/v1/orders/{id}` | ❌ | `createdAt` llega como epoch ms; app espera ISO | Backend | `write-dates-as-timestamps: false` |

Por defecto se corrige el **backend**. Solo se propone cambiar la app si el contrato de la app es claramente defectuoso o el usuario lo pide.

## 4. Cambios en la app (solo con confirmación explícita)

- Base URL por entorno (`expect/actual` o BuildKonfig) apuntando al backend local/producción.
- Config de red de desarrollo: `network_security_config` (Android) y ATS (iOS) para HTTP local.
- Si se acordó compartir DTOs: mover DTOs a un módulo `shared-api` que pueda consumir también el backend (solo si el backend es Kotlin/JVM y el usuario lo aprueba).

## 5. Prueba extremo a extremo

1. `docker compose up -d`.
2. Ejecutar la app en emulador/simulador contra el backend local.
3. Recorrer los flujos principales (login, listados, crear, editar, borrar) y revisar `docker compose logs -f api` buscando 4xx/5xx inesperados.
4. Si existen tests de la capa de red en la app (`commonTest` con MockEngine), considerar añadir fixtures con respuestas reales del backend.

Terminado cuando el informe no tiene filas ❌.
