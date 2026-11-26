# Módulo `androidApp`

Entrada Android de la app KMP. No contiene lógica de negocio: crea el `AppContainer` y muestra la navegación compartida.

## Archivos que importan

- [`src/main/kotlin/com/example/librerias/MainActivity.kt`](src/main/kotlin/com/example/librerias/MainActivity.kt): `ComponentActivity` que llama a `AppNavHost(container)`.
- [`src/main/AndroidManifest.xml`](src/main/AndroidManifest.xml): permiso `INTERNET` y referencia a `@xml/network_security_config`.
- [`src/main/res/xml/network_security_config.xml`](src/main/res/xml/network_security_config.xml): HTTP claro **solo** para `10.0.2.2` y `localhost`; no añadir dominios de producción.
- La URL base `http://10.0.2.2:8080/api/v1/` está en [`../shared/src/androidMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt`](../shared/src/androidMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt). `10.0.2.2` es la IP con la que el emulador alcanza al host; `localhost` dentro del emulador sería el propio emulador.

## Comandos

Desde `Librerias_eduvia/`:

```powershell
gradlew.bat :androidApp:assembleDebug   # genera el APK
gradlew.bat :androidApp:installDebug    # instala en el emulador conectado
```

El APK queda en [`build/outputs/apk/debug/androidApp-debug.apk`](build/outputs/apk/debug/androidApp-debug.apk). La API debe estar levantada (`docker compose up -d --build --wait` desde la raíz) para que login, tareas y juntas carguen datos reales.

## Configuración

`minSdk=24`, Ktor 3.2.1 y Compose Multiplatform se fijan en [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml). Durante la Fase 6 se usó `adb reverse` de forma temporal porque `httpd.exe` ocupaba el 8080 del host; se revirtió y la URL definitiva sigue siendo `10.0.2.2`. Ver [`../../docs/backend/DECISIONS.md`](../../docs/backend/DECISIONS.md) decisión 13 y [`../../docs/backend/PROGRESS.md`](../../docs/backend/PROGRESS.md).
