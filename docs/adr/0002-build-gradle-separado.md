# ADR 0002 — Build de Gradle separado para el backend

## Estado

Aceptada.

## Contexto

La app KMP ya tiene su propio build en `Librerias_eduvia/settings.gradle.kts`, con plugins de Android, Compose Multiplatform y targets de iOS. La guía (`GUIA-OPENCODE.md`) avierte expresamente de no mezclar ese build con el de Spring Boot: los plugins de Android/KMP y los de Boot compiten por el mismo classpath y ralentizan la sincronización de Android Studio. En Windows, además, el wrapper que se usa es `gradlew.bat`, no `./gradlew`.

Las decisiones 1 y 2 de [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md) fijan que el backend vive en `backend/` y está escrito en Kotlin; la decisión 4 fija el paquete `com.example.librerias.api`.

## Decisión

`backend/` es un **build de Gradle independiente**: tiene su propio `settings.gradle.kts` (`rootProject.name = "api"`), su `build.gradle.kts`, su wrapper (`gradlew.bat`) y su carpeta `gradle/`. No se incluye como subproyecto en `Librerias_eduvia/settings.gradle.kts`.

El build fija Java 21 con `JavaLanguageVersion.of(21)`, Kotlin 2.4.21, Spring Boot 4.2.0, springdoc 3.2.1, JaCoCo con umbral del 80 % y dos tareas de prueba diferenciadas: `test` (excluye `@Tag("e2e")`) y `e2eTest` (solo esa etiqueta, contra un stack levantado). La ruta de los fixtures se pasa como propiedad del sistema desde `../contract/fixtures`.

El porqué: separar los builds mantiene estable la sincronización de Android Studio, permite fijar versiones de Boot sin alterar `gradle/libs.versions.toml` de la app y hace que cada módulo tenga un único comando de verificación. [`scripts/test-all.ps1`](../../scripts/test-all.ps1) es quien orquesta ambos builds en Windows, sin mezclarlos.

## Consecuencias

### Positivas

- `cd backend && gradlew.bat check` ejecuta pruebas, contrato y cobertura del backend sin tocar la app.
- `cd Librerias_eduvia && gradlew.bat :shared:testAndroid` ejecuta las pruebas KMP con sus propias versiones (Ktor 3.2.1).
- Las versiones de Spring Boot 4 y de las dependencias de test (rest-assured 6.3.0, mockk 1.14.12) se pueden fijar en un solo archivo, [`backend/build.gradle.kts`](../../backend/build.gradle.kts).
- Los errores típicos de Boot 4 (Jackson 3, nueva ruta de `TestRestTemplate`, retirada de `write-dates-as-timestamps`) quedan confinados al backend; están registrados en [`docs/backend/PROGRESS.md`](../backend/PROGRESS.md).

### Negativas

- Hay dos wrappers y dos configuraciones de Gradle que mantener; los comandos deben ejecutarse desde el directorio correcto.
- La cobertura y los reportes se generan por separado: JaCoCo en `backend/build/reports/jacoco/` y los reportes KMP en `Librerias_eduvia/build/`.
- La guía original usa nombres de la plantilla (`composeApp/`, `scripts/test-all.sh`, `:shared:testDebugUnitTest`) que no coinciden con este repo (H7, sin corregir). Los nombres reales son `Librerias_eduvia/`, `scripts/test-all.ps1` y `:shared:testAndroid`. Ver [`GUIA-OPENCODE.md`](../../GUIA-OPENCODE.md), [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H7 y [`scripts/test-all.ps1`](../../scripts/test-all.ps1).

## Alternativas consideradas

- **Añadir `backend` a `settings.gradle.kts` de la app:** descartada por los conflictos de plugins y la sincronización lenta que indica la guía.
- **Usar Maven para el backend:** descartada porque la guía, Initializr y el resto del repo usan Gradle con Kotlin DSL.
- **Un único build compuesto (composite build):** descartada por complejidad adicional; el script `test-all.ps1` ya coordina los dos builds de forma explícita.

## Enlaces

- Decisiones: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), filas 1, 2 y 4.
- Build del backend: [`backend/settings.gradle.kts`](../../backend/settings.gradle.kts), [`backend/build.gradle.kts`](../../backend/build.gradle.kts), [`backend/gradlew.bat`](../../backend/gradlew.bat).
- Build de la app: [`Librerias_eduvia/settings.gradle.kts`](../../Librerias_eduvia/settings.gradle.kts), [`Librerias_eduvia/gradle/libs.versions.toml`](../../Librerias_eduvia/gradle/libs.versions.toml).
- Orquestación: [`scripts/test-all.ps1`](../../scripts/test-all.ps1).
- Hallazgo documentado: [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H7.
