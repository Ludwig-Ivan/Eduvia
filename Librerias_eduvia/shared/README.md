# Módulo `shared` (EduvIA)

Código común de la app KMP: modelos, dominio, UI Compose y capa de red Ktor. Android e iOS comparten este módulo; cada plataforma aporta solo su motor HTTP y su URL base.

## Source sets reales

| Source set | Para qué sirve |
|---|---|
| `commonMain` | Modelos, casos de uso, repositorios, DTOs, `ApiClient`, UI y navegación |
| `androidMain` | Motor OkHttp y base URL `http://10.0.2.2:8080/api/v1/` |
| `iosMain` | Motor Darwin y base URL `http://localhost:8080/api/v1/` |
| `commonTest` | Pruebas del cliente con `MockEngine` y de repositorios/casos de uso |
| `androidHostTest` | Pruebas de contrato con los JSON de `contract/fixtures/` |
| `iosTest` | Prueba mínima de la lógica compartida en iOS |

No existe `jvmMain` en este repo (H6): el README genérico de la app lo mencionaba, pero los source sets son los de arriba. Ver [`../../docs/HALLAZGOS.md`](../../docs/HALLAZGOS.md) H6.

## Capas

- `model/` y `domain/`: modelos de dominio y casos de uso; no conocen HTTP.
- `data/remote/`: `ApiClient` (inyección de engine, refresh en 401, `ApiException` con `ProblemDto`), 31 DTOs `@Serializable`, `Mappers`, `SessionStore` (en memoria), `RemoteListStore` y los 5 repositorios remotos.
- `data/repository/`: implementaciones en memoria que siguen en el código. `di/AppContainer.kt` inyecta las remotas; ⚠️ Por confirmar si se conservan como ejemplo o se retiran: [`../../docs/backend/OPEN-QUESTIONS.md`](../../docs/backend/OPEN-QUESTIONS.md) Q12 y [`../../docs/HALLAZGOS.md`](../../docs/HALLAZGOS.md) H10.
- `ui/` y `navigation/`: pantallas Compose y `AppNavHost`; Android y iOS solo delegan en ellas.

## Dependencias y pruebas

Ktor **3.2.1** (la 3.2.0 fallaba en D8), serialización 1.9.0 y `minSdk=24`; las versiones están en [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml) y las dependencias en [`build.gradle.kts`](build.gradle.kts).

Desde `Librerias_eduvia/`:

```powershell
gradlew.bat :shared:testAndroid          # la tarea que usa scripts/test-all.ps1 (19 tests)
gradlew.bat :shared:testAndroidHostTest  # solo las pruebas host de Android
```

Las pruebas de contrato leen los mismos 11 archivos que el backend desde [`../../contract/fixtures/`](../../contract/fixtures/), con `ignoreUnknownKeys = false` para detectar cambios de forma. El resultado está en [`../../docs/backend/compat-report.md`](../../docs/backend/compat-report.md); para entender las pantallas, ver [`../../docs/guias/app-kotlin.md`](../../docs/guias/app-kotlin.md).
