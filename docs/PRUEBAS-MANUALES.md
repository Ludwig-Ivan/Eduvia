# Guía de pruebas con la API y la base de datos levantadas en Docker

Propósito: dejar **API + PostgreSQL corriendo** mientras se ejecutan pruebas E2E, se hacen peticiones manuales o se prueba la app en el emulador. Todo se hace desde la raíz del repositorio en Windows (PowerShell).

> En este equipo el puerto `8080` lo ocupa un Apache ajeno: **no lo detengas**. Usa siempre un puerto alternativo y un nombre de proyecto propio (`-p ...`). La guía usa `miapp-manual` con `18088` (API), `15432` (PostgreSQL) y `18089` (Adminer), que no chocan con `miapp-test` (18080/15432/18081) de [`scripts/test-all.ps1`](../scripts/test-all.ps1).

## 0. Preparación (una vez)

```powershell
# Docker Desktop debe estar corriendo
docker version

# .env debe existir: copiar la plantilla y revisar JWT_SECRET (>= 32 caracteres)
Copy-Item .env.example .env   # solo si aún no existe
notepad .env
```

`SPRING_PROFILES_ACTIVE=dev` carga la semilla `backend/src/main/resources/db/dev-data/V1000__seed.sql` (usuario demo `laura@eduvia.mx` / `Maestra123`). Si falta `JWT_SECRET`, Compose falla en el arranque a propósito.

## 1. Levantar el stack y dejarlo corriendo

```powershell
$env:API_PORT = '18088'
$env:DB_PORT = '15432'
$env:ADMINER_PORT = '18089'
docker compose -p miapp-manual config -q
docker compose -p miapp-manual up -d --build --wait
docker compose -p miapp-manual ps
```

`--wait` solo termina cuando PostgreSQL responde `pg_isready` y la API responde `/actuator/health`. Para incluir Adminer, añade `--profile tools` al `up`.

Comprobaciones rápidas:

```powershell
curl.exe -fsS http://localhost:18088/actuator/health
curl.exe -fsS http://localhost:18088/v3/api-docs
docker compose -p miapp-manual logs -f api
```

La salud esperada es `{"groups":["liveness","readiness"],"status":"UP"}`; Swagger UI está en `http://localhost:18088/swagger-ui.html`.

## 2. Pruebas automáticas contra el stack levantado

```powershell
# E2E (health, login + /me, CRUD de grupo/tarea, ProblemDetail 404)
cd backend
$env:E2E_BASE_URL = 'http://localhost:18088'
.\gradlew.bat --no-daemon e2eTest
cd ..
```

Si no hace falta mantener el stack, la forma corta es `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-all.ps1`: valida compose, corre backend (`check`), app KMP y E2E, y **baja** el stack automáticamente en `finally`.

Opcionales, sin Docker:

```powershell
cd backend
.\gradlew.bat --no-daemon check
cd ..\Librerias_eduvia
.\gradlew.bat --no-daemon :shared:testAndroid :shared:assembleAndroidMain :androidApp:assembleDebug
```

## 3. Peticiones manuales a la API

```powershell
$base = 'http://localhost:18088/api/v1'
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" `
  -ContentType 'application/json' `
  -Body '{"email":"laura@eduvia.mx","password":"Maestra123"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

Invoke-RestMethod -Uri "$base/me" -Headers $headers
Invoke-RestMethod -Uri "$base/groups?size=10" -Headers $headers
Invoke-RestMethod -Uri "$base/tasks?groupId=1&size=10" -Headers $headers
Invoke-RestMethod -Uri "$base/meetings?groupId=1&size=10" -Headers $headers
```

Respuestas esperables: `200` en las cuatro; sin token, `/me` debe devolver `401` con `WWW-Authenticate: Bearer`. Un `409` al crear juntas significa duplicado de `group_id + fecha + hora + lugar`.

## 4. Inspeccionar la base de datos

```powershell
# psql dentro del contenedor
docker compose -p miapp-manual exec postgres psql -U app -d app -c 'select count(*) from users;'
docker compose -p miapp-manual exec postgres psql -U app -d app -c 'select id, title, due_date from school_tasks order by due_date;'
docker compose -p miapp-manual exec postgres psql -U app -d app -c 'select id, date, time, location from meetings;'
```

Adminer (si se levantó con `--profile tools`): `http://localhost:18089`, servidor `postgres`, base `app`, usuario/contraseña los de `.env`.

> Las 6 tareas semilla vencen entre el 2 y el 15 de octubre de 2026: con la fecha actual, la pestaña «Activas» de la app aparece vacía y «Pasadas» muestra las 6. Es el comportamiento esperado, no un fallo.

## 5. Probar la app en el emulador contra ese stack

La base URL de Android está fijada en el código a `http://10.0.2.2:8080/api/v1/`. No la cambies: redirige dentro del emulador (sin tocar el Apache del host):

```powershell
$adb = 'C:\Users\Dev_Ludwig\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices                       # debe aparecer emulator-xxxx  device
& $adb root
& $adb reverse tcp:8080 tcp:18088
& $adb shell iptables -t nat -A OUTPUT -d 10.0.2.2 -p tcp --dport 8080 -j REDIRECT --to-ports 8080

cd Librerias_eduvia
.\gradlew.bat --no-daemon :androidApp:installDebug
cd ..
```

Abre la app e inicia sesión con `laura@eduvia.mx` / `Maestra123`. Recorre Inicio, Tareas, Calendario y Juntas; comprueba los cambios en `docker compose -p miapp-manual logs api` (no deben aparecer 4xx/5xx inesperados) y en psql.

Limpieza de la redirección al terminar:

```powershell
$adb = 'C:\Users\Dev_Ludwig\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell iptables -t nat -D OUTPUT -d 10.0.2.2 -p tcp --dport 8080 -j REDIRECT --to-ports 8080
& $adb reverse --remove tcp:8080
& $adb unroot
```

Si `adb root` no está disponible en tu AVD, comprueba la alternativa documentada en [`operacion.md`](operacion.md#prueba-manual-con-stack-aislado) o salta esta sección y valida la API con `curl`/Swagger desde el host (sección 3). **`API_PORT=8080` no es una opción en este equipo**: el Apache ajeno ocupa ese puerto.

## 6. Control de errores con API/PostgreSQL apagados

La app distingue «sin conexión», «timeout», «servidor no disponible», «credenciales incorrectas», «validación», «no encontrado» y «conflicto». El texto se decide en un único mapper (`ApiErrors.kt`); la UI nunca muestra excepciones de Ktor/OkHttp ni URLs internas.

Con la app ya abierta y la sesión iniciada:

```powershell
# 1. API y BD apagadas
docker compose -p miapp-manual stop api postgres
# En la app: cerrar sesión e iniciar sesión de nuevo →
# "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."
# En Tareas: banner "Sin conexión" con el mismo mensaje y botones Reintentar/Entendido.
# Pulsar Reintentar con los contenedores apagados → se mantiene el mismo mensaje.

# 2. Solo PostgreSQL apagado (API arriba)
docker compose -p miapp-manual start api
docker compose -p miapp-manual stop postgres
# Login → "La conexión tardó demasiado. Revisa tu internet e inténtalo de nuevo."
# (el request expira a los 15 s; la app no se queda en "Entrando…").

# 3. Restaurar y comprobar
docker compose -p miapp-manual start postgres api
curl.exe -s http://localhost:18088/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

Tras un `adb reboot` o `adb root` se pierde la redirección: repetir los comandos de `adb root`, `adb reverse tcp:8080 tcp:18088` y la regla `iptables` de la sección 5 antes de repetir la prueba. Si `10.0.2.2:8080` responde con `Server: Apache`, la regla no está activa; sin ella el login puede devolver 404 o un timeout engañoso.

Otros mensajes verificados por contrato (tests con `MockEngine` en `ApiErrorsTest.kt`): 5xx → «El servidor no está disponible en este momento. Inténtalo de nuevo en unos minutos.», 401 en login → «Correo o contraseña incorrectos. Revisa tus datos o recupera tu contraseña.», 404 → «No encontramos lo que buscabas. Puede que ya no esté disponible.»

## 7. Bajar y reiniciar desde cero

```powershell
docker compose -p miapp-manual down            # conserva los datos
docker compose -p miapp-manual down -v --remove-orphans   # borra el volumen: próxima carga vuelve a sembrar
```

No edites migraciones ya aplicadas: si sale `FlywayValidateException: checksum mismatch`, restaura el archivo original y usa `down -v`.

## 8. Resultado de la verificación en Android (ADB)

Ejecutada con `emulator-5554`, `adb root`, `adb reverse --list` → `host-17 tcp:8080 tcp:18088` y el stack `miapp-manual` en `18088/15432`. Ambos contenedores en estado `healthy` y `/actuator/health` en `UP`.

- **Sesión:** Bienvenida → login de `laura@eduvia.mx` y de `maria@correo.com`; Inicio, Tareas, Calendario y Juntas muestran datos de la API (6 tareas, 2 juntas, eventos reales y asistencia real).
- **CRUD docente:** alta → edición → borrado de una junta y de una tarea desde la UI, confirmados en `GET /meetings`, `GET /tasks`, `GET /calendar/events` y en psql. La BD terminó con 2 juntas, 6 tareas y 22 eventos; la tarea creada mostró `teacherName = Laura Hernández` (usuario de la sesión) y el grupo correcto.
- **Padre:** cambio de asistencia (`ATTENDING` → `DECLINED` → `ATTENDING`) persistido en `meeting_responses(1,1)`.
- **Recuperación:** en `dev`, `POST /auth/reset-code` devuelve el código y la app lo muestra en `📩 Correo simulado (solo demo)`; el cambio de contraseña desde la UI se confirmó con `POST /auth/login`. La contraseña demo se restauró a `Maestra123` al terminar.
- **Logs:** sin `ERROR`/excepciones nuevas en la API ni en `logcat` tras las correcciones de fecha y de borrado de eventos.
- **Control de errores (verificación final, sección 6):** con API+BD apagadas, login y banner de Tareas mostraron el mensaje de conexión; al pulsar «Reintentar» se mantuvo el mismo texto. Con solo PostgreSQL apagado, el login mostró el mensaje de timeout tras ~15 s. Con todo restaurado, el login entró al Inicio con 5 tareas, 3 juntas y 23 eventos; Flyway quedó en `1`, `2`, `1000` con `success = true`. Los contenedores se dejaron `healthy` y `/actuator/health` en `UP`.

> `app.reset-code-in-response: true` está activado **solo** en `application-dev.yml`; en producción el código no se expone en la respuesta.

## Referencias

- [`pruebas.md`](pruebas.md) — pirámide de pruebas, suites y contrato de fixtures.
- [`operacion.md`](operacion.md) — runbook, credenciales demo y solución de problemas.
- [`docker.md`](docker.md) — servicios, puertos y variables de Compose.
- [`REFACTOR-RESUMEN.md`](REFACTOR-RESUMEN.md) — resultado de la verificación final de la refactorización.
