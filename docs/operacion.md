# Operación — EduvIA

> Runbook para arrancar, inspeccionar y resolver incidencias del stack local. Detalles de servicios en [`docker.md`](docker.md), verificación automática en [`pruebas.md`](pruebas.md) y datos en [`base-de-datos.md`](base-de-datos.md).

## Arranque y parada

```powershell
# Desde la raíz: .env debe existir (copiar .env.example) y JWT_SECRET estar definido
docker compose config -q
docker compose up -d --build --wait
docker compose ps
docker compose down            # conserva pgdata
docker compose down -v --remove-orphans   # borra los datos (usado por las pruebas)
```

`--wait` solo termina cuando `postgres` responde `pg_isready` y `api` responde `/actuator/health`. El perfil Spring por defecto es `dev`, que carga la semilla de `backend/src/main/resources/db/dev-data/V1000__seed.sql` (nombre real; [H1](HALLAZGOS.md)). Para un entorno sin datos demo, cambia `SPRING_PROFILES_ACTIVE` en `.env` a `prod`.

## Salud, Swagger y base de datos

```powershell
curl.exe -fsS http://localhost:8080/actuator/health
curl.exe -fsS http://localhost:8080/v3/api-docs
docker compose logs -f api
docker compose logs postgres
docker compose exec postgres psql -U app -d app
```

Respuesta de salud verificada: `{"groups":["liveness","readiness"],"status":"UP"}`. Swagger UI: `http://localhost:8080/swagger-ui.html`. Adminer (perfil `tools`): `docker compose --profile tools up -d` → `http://localhost:8081`, servidor `postgres`, usuario `app` y la contraseña de `.env`.

Consultas rápidas y reinicio de datos:

```sql
select count(*) from users;
select id, title, due_date from school_tasks order by due_date;
select id, date, time, location from meetings;
select id, type from calendar_events where id >= 1000 order by id;
```

```powershell
# Volver al estado inicial sembrado: borrar volumen y reconstruir con perfil dev
docker compose down -v
docker compose up -d --build --wait
```

Credenciales demo: `laura@eduvia.mx` / `Maestra123` (`TEACHER`) y `maria@correo.com` / `Padre1234` (`PARENT`).

## Prueba manual con stack aislado

El puerto 8080 del host lo ocupa `httpd.exe` (PID 5240): **no lo detengas**. La prueba manual de la Fase 6 usó `miapp-manual` con `API_PORT=18088`, `DB_PORT=15435` y `ADMINER_PORT=18089`, y una redirección temporal del emulador:

```powershell
docker compose -p miapp-manual up -d --build --wait
docker compose -p miapp-manual ps
curl.exe -fsS http://localhost:18088/actuator/health
adb reverse tcp:8080 tcp:18088
cd Librerias_eduvia
.\gradlew.bat --no-daemon :androidApp:installDebug
```

Recorrido verificado: login, listado de tareas y juntas, detalle, crear/editar/borrar tarea y junta, comprobando cada cambio en la API y en PostgreSQL. Al terminar **se revirtió todo**: base URL Android restaurada a `http://10.0.2.2:8080/api/v1/`, `adb reverse --remove-all` y `docker compose -p miapp-manual down -v --remove-orphans`. El script automático usa otro stack, `miapp-test` (18080/15432/18081); su ejecución está en [`pruebas.md`](pruebas.md).

## Solución de problemas

| Síntoma | Causa y acción |
|---|---|
| `401` + `WWW-Authenticate: Bearer` en la app | Token ausente, expirado (900 s) o refresh ya rotado/revocado. El cliente Ktor renueva y reintenta; comprueba `JWT_SECRET` y que no se haya reiniciado con otro secreto. Sin token, `GET /api/v1/me` debe dar 401 (verificado). |
| `409` al crear/editar una junta | Ya existe `group_id + date + time + location` (comparación case-insensitive) por `uk_meetings_unique`. Cambia fecha, hora o lugar. Al **editar**, el backend excluye la propia junta de la comprobación; si sale el error, la app está llamando a creación en vez de `PUT /meetings/{id}` (fallo corregido en Fase 6). |
| `GET /tasks` sin `groupId` | El endpoint exige `groupId`; usa `GET /api/v1/tasks?groupId=1`. El 500 visto en una prueba manual fue un error del `curl` de prueba, no del backend (ver Fase 6 de [`backend/PROGRESS.md`](backend/PROGRESS.md)). |
| «Activas» vacía, «Pasadas» con 6 tareas | Las 6 tareas semilla vencen entre el 2 y el 15 de octubre de 2026; a fecha 25 de noviembre de 2026 es el resultado esperado, no un fallo. Consulta `due_date` con psql o cambia las fechas en datos de prueba, pero no modifiques el seed versionado sin una migración nueva. |
| `port is already allocated` en 8080 | `httpd.exe` (PID 5240) ocupa el puerto. No lo detengas: usa `API_PORT`, `DB_PORT` y `ADMINER_PORT` alternativos y `-p miapp-manual`/`-p miapp-test`. |
| D8 falla con `Space characters in SimpleName ... io.ktor.client.plugins.Messages` | Ktor **3.2.0** no pasa D8; la versión verificada es **3.2.1** (Fase 5 de [`backend/PROGRESS.md`](backend/PROGRESS.md)). No vuelvas a 3.2.0. |
| `Schema-validation: missing column` al arrancar | Las entidades JPA no coinciden con Flyway. Revisa la migración nueva y `ddl-auto: validate`; no cambies el esquema a mano en la BD. |
| `FlywayValidateException: checksum mismatch` | Se editó una migración ya aplicada (por ejemplo V1). Restaura el archivo original; en desarrollo local, `docker compose down -v` y vuelve a levantar. Relacionado con [H1](HALLAZGOS.md): el seed real es `V1000__seed.sql`. |
| `Could not find a valid Docker environment` en tests | Docker Desktop no está corriendo; los tests de integración necesitan Testcontainers con PostgreSQL real. |
| `CLEARTEXT communication not permitted` o `Failed to connect to /10.0.2.2:8080` en el emulador | Revisa `Librerias_eduvia/androidApp/src/main/AndroidManifest.xml`, `Librerias_eduvia/androidApp/src/main/res/xml/network_security_config.xml` y que la API esté levantada en el puerto esperado. |
| El contrato «cambió» en las pruebas | Si el cambio es intencional: `cd backend` y `.\gradlew.bat --no-daemon test -PupdateContract=true`, y adapta los DTOs de la app; si no, corrige la respuesta del backend. |

## Límites conocidos

Los hallazgos registrados en [`HALLAZGOS.md`](HALLAZGOS.md) **no se han arreglado**: entre otros, el nombre incorrecto del seed en `data-model.md` (H1), el recuento de schemas del informe de compatibilidad (H3) y el desglose de operaciones del contrato (H4). Las decisiones pendientes están marcadas como `⚠️ Por confirmar` en [`backend/OPEN-QUESTIONS.md`](backend/OPEN-QUESTIONS.md).

> ⚠️ Por confirmar: este runbook cubre el entorno local verificado. No hay evidencia en las fuentes de un procedimiento de despliegue a producción (secretos, TLS, perfil `prod`); ver [`backend/OPEN-QUESTIONS.md`](backend/OPEN-QUESTIONS.md) antes de inventar uno.
