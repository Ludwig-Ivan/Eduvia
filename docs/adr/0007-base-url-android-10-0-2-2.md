# ADR 0007 — Base URL Android `10.0.2.2` y HTTP claro solo en desarrollo

## Estado

Aceptada.

## Contexto

La app KMP usa Ktor **3.2.1** con `minSdk=24` (decisión 11: la versión 3.2.0 fallaba en D8 al dexar el APK con `Messages`/`use streaming syntax`). La URL base se define con `expect/actual` en la capa compartida: [`PlatformHttpClient.kt` de Android](../../Librerias_eduvia/shared/src/androidMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt) usa el motor OkHttp y el de iOS usa Darwin.

Durante la prueba manual de Fase 6, `httpd.exe` (PID 5240) ocupaba el puerto 8080 del host y no se tocó. Se levantó un stack aislado con `API_PORT=18088` y se usó temporalmente `adb reverse tcp:8080 tcp:18088` junto a una base URL Android `http://localhost:8080/api/v1/`. La decisión 13 de [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md) recoge que esa combinación era temporal y que se revirtió al cerrar la fase.

## Decisión

La base URL definitiva en Android es **`http://10.0.2.2:8080/api/v1/`**; en iOS es **`http://localhost:8080/api/v1/`**. El porqué: el emulador Android tiene su propia pila de red, así que `localhost` dentro del emulador se refiere al propio emulador; `10.0.2.2` es la IP alias con la que alcanza al equipo anfitrión donde corre la API. El simulador iOS, en cambio, comparte la red del host y llega con `localhost`.

El tráfico HTTP sin cifrar se permite **solo** para `10.0.2.2` y `localhost` en [`network_security_config.xml`](../../Librerias_eduvia/androidApp/src/main/res/xml/network_security_config.xml), referenciado desde el [`AndroidManifest.xml`](../../Librerias_eduvia/androidApp/src/main/AndroidManifest.xml) junto al permiso `INTERNET`. En iOS se usa la excepción local de ATS (`NSAllowsLocalNetworking = true`) en [`Info.plist`](../../Librerias_eduvia/iosApp/iosApp/Info.plist), sin activar `NSAllowsArbitraryLoads`.

La configuración temporal con `adb reverse` se deshizo (`adb reverse --remove-all`) y la URL de Android se restauró a `10.0.2.2`; los puertos del stack definitivo siguen siendo configurables en [`docker-compose.yml`](../../docker-compose.yml) y los fija [`scripts/test-all.ps1`](../../scripts/test-all.ps1) para sus propios stacks aislados.

## Consecuencias

### Positivas

- La app funciona en el emulador sin depender de un `adb reverse` permanente ni de desplazar un servicio ajeno del puerto 8080.
- El dominio permitido en claro está restringido a dos hosts de desarrollo; cualquier otra conexión debe ir por HTTPS.
- La prueba manual verificó login, listar, crear, editar y borrar tareas y juntas contra la API real, con persistencia confirmada en PostgreSQL; ver [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md), Fase 6.
- iOS mantiene ATS activo para todo lo que no sea red local.

### Negativas

- `10.0.2.2` solo tiene sentido en el emulador; un dispositivo físico o una red distinta exige otra configuración (por ejemplo, `adb reverse`, documentado en la guía).
- Hay dos URLs base distintas que mantener, una por plataforma, y dos archivos de configuración de red (`network_security_config.xml` e `Info.plist`).
- La URL temporal de Fase 6 podría confundir a quien lea solo lógica de prueba antigua; la reversión está registrada en `PROGRESS.md` y en la decisión 13.

## Alternativas consideradas

- **Usar `localhost:8080` en Android:** descartada; dentro del emulador apunta al emulador, no al host. Solo se usó de forma temporal con `adb reverse` y se revirtió.
- **Dejar `adb reverse` como configuración definitiva:** descartada; depende de un comando manual y del puerto libre del host, y no era la URL acordada para la app.
- **Permitir cleartext para todos los dominios:** descartada por seguridad; se limita a los dos hosts de desarrollo.
- **Forzar HTTPS en local:** descartada porque el stack de desarrollo publica HTTP en `8080` y añadir certificados no aportaría nada a la prueba local.

## Enlaces

- Decisiones: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), filas 11 y 13; [`docs/backend/OPEN-QUESTIONS.md`](../backend/OPEN-QUESTIONS.md).
- Código de la app: [`PlatformHttpClient.kt` (Android)](../../Librerias_eduvia/shared/src/androidMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt), [`PlatformHttpClient.kt` (iOS)](../../Librerias_eduvia/shared/src/iosMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt), [`AndroidManifest.xml`](../../Librerias_eduvia/androidApp/src/main/AndroidManifest.xml), [`network_security_config.xml`](../../Librerias_eduvia/androidApp/src/main/res/xml/network_security_config.xml), [`Info.plist`](../../Librerias_eduvia/iosApp/iosApp/Info.plist).
- Red y puertos: [`docker-compose.yml`](../../docker-compose.yml), [`scripts/test-all.ps1`](../../scripts/test-all.ps1).
- Evidencia: [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md), [`docs/backend/compat-report.md`](../backend/compat-report.md).
