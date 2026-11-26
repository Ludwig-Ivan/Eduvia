# Módulo `iosApp`

Entrada iOS de la app KMP. Es una carcasa SwiftUI: la interfaz real es Compose Multiplatform del módulo `shared`.

## Archivos que importan

- [`iosApp/iOSApp.swift`](iosApp/iOSApp.swift): punto de entrada `@main`, muestra `ContentView`.
- [`iosApp/ContentView.swift`](iosApp/ContentView.swift): puente `UIViewControllerRepresentable` hacia `MainViewControllerKt.MainViewController()`.
- [`iosApp/Info.plist`](iosApp/Info.plist): excepción ATS local (`NSAllowsLocalNetworking = true`), sin `NSAllowsArbitraryLoads`.
- [`Configuration/Config.xcconfig`](Configuration/Config.xcconfig): `TEAM_ID`, `PRODUCT_NAME`, bundle id y versiones.
- Proyecto Xcode: [`iosApp.xcodeproj/project.pbxproj`](iosApp.xcodeproj/project.pbxproj).

La URL base `http://localhost:8080/api/v1/` y el motor Darwin están en [`../shared/src/iosMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt`](../shared/src/iosMain/kotlin/com/example/librerias/data/remote/PlatformHttpClient.kt). El simulador comparte la red del host, por lo que `localhost` sí alcanza la API.

## Compilación

Las pruebas del simulador iOS no se ejecutan en Windows, pero sí se comprueba la compilación desde `Librerias_eduvia/`:

```powershell
gradlew.bat :shared:compileKotlinIosArm64
gradlew.bat :shared:compileTestKotlinIosArm64
```

La ejecución en simulador/dispositivo se hace desde Xcode con el proyecto de este directorio. La compatibilidad de la capa de red está en [`../../docs/backend/compat-report.md`](../../docs/backend/compat-report.md) y la decisión de la URL en [`../../docs/backend/DECISIONS.md`](../../docs/backend/DECISIONS.md) (base URL por plataforma).
