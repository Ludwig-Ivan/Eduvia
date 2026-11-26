# Decisiones del backend

| # | Decisión | Valor | Origen |
|---|---|---|---|
| 1 | Ubicación del backend | `backend/` dentro de este repo | Default de la guía, confirmado por el usuario |
| 2 | Lenguaje del backend | Kotlin | Default del orquestador, confirmado por el usuario |
| 3 | Autenticación | JWT completo (Fase 3b): login/registro/refresh/logout, Bearer, `JWT_SECRET` | Confirmado por el usuario (la app tiene login) |
| 4 | Paquete base | `com.example.librerias.api` | Derivado del namespace `com.example.librerias` de la app |
| 5 | Nombre del proyecto Docker Compose | `miapp` | Default de la guía, confirmado por el usuario |
| 6 | Herramientas de UI (Adminer) | Perfil opcional `tools` | Default de la guía, confirmado por el usuario |
| 7 | Evento de calendario de una junta | `id = 1000 + meetings.id` (las tareas usan su propio id) | Ids solapados en las dos listas de la app; ver `data-model.md` |
| 8 | Formato de `dateText` en solicitudes | `YYYY-MM-DD` (ISO, `LocalDate`) | Backend implementado así; el `openapi.yaml` se corrigió en Fase 5 |
| 9 | Sesión en la app KMP | En memoria (`SessionStore`); sin persistencia segura aún | Fuera del alcance de esta fase; documentado en `compat-report.md` |
| 10 | Registro con hijos/grupos | `RegisterUseCase` envía listas vacías; luego se vinculan con endpoints separados | Limitación del flujo actual de la app; documentada en `compat-report.md` |
| 11 | Versión de Ktor en la app | **3.2.1** (3.2.0 falla en D8 con `Messages`/`use streaming syntax`); `minSdk=24` | Fallo real al dexar el APK en Fase 6; versión verificada en Maven Central |
| 12 | Edición de juntas | `PUT /meetings/{id}` (M9): conserva el id, excluye la propia junta del chequeo de duplicados y actualiza el evento `1000+id` | Fallo manual reproducido en Fase 6; el usuario eligió «Arreglar la app» ampliando el alcance de capa de red a dominio/UI |
| 13 | Base URL Android durante la prueba manual | Temporal `http://localhost:8080/api/v1/` con `adb reverse tcp:8080 tcp:18088`; definitiva `http://10.0.2.2:8080/api/v1/` | `httpd.exe` (PID 5240) ocupa 8080 en el host y no se tocó; reversión verificada al cerrar Fase 6 |
