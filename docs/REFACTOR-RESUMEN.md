# Refactorización conservadora de EduvIA — resumen final

Rama: `refactor/limpieza` · Punto de retorno: `v0.1.0` · Último commit de código: `3692c32`.
Seguimiento detallado, tablas de hallazgos e incidencias: [`GUIA-REFACTORIZACION.md`](../GUIA-REFACTORIZACION.md).

## Resumen por categoría

| Categoría | Hallazgos aplicados | Commits | Archivos tocados | Qué se cambió |
|---|---|---|---|---|
| 4.1 Código muerto (🟢) | 7 (M-01…M-07) | 9 | 9 | Imports sin uso en backend y app (4), código comentado ya resuelto (2), TODOs de copiar/compartir aclarados sin implementarlos (1). |
| 4.1 Código muerto / Gradle (🟡) | 4 (M-08…M-11) | 4 | 7 | 3 métodos de repositorio y `attendanceSummary` sin llamadas; MockK del backend y 6 aliases/versiones huérfanos del catálogo. Se actualizaron los documentos que los citaban en el mismo commit. |
| 4.2 Innecesario o sobrediseñado | 6 (N-01…N-06) | 6 | 15 | `MAX_PAGE_SIZE` y `pageRequest` compartidos en `web/Pagination.kt`; generador de códigos en `GroupCodes.kt`; `parseDate` en `DateParsing.kt`; `HeaderGradient` en el tema; declaración duplicada de `foundation` eliminada. Mismos valores, orden y mensajes de error. |
| 4.3 Legibilidad | 3 (L-01…L-03) | 3 | 4 | Nombres fully-qualified sustituidos por imports; cifra histórica de bloques Mermaid aclarada; cinco funciones largas divididas en helpers privados sin reordenar validaciones ni escrituras. |
| 4.4 Responsabilidades | 0 | — | — | Sin hallazgos: controllers, services, repositorios, mappers y capas de la app ya cumplen la separación. R-01 quedó descartado. |

Los 20 hallazgos están publicados (🚀); ninguno se revertió (↩️). No se tocó el diseño visual, los nombres de campos/rutas/`@SerialName`/`@JsonProperty`, los fixtures ni las migraciones.

## Línea base completa (antes / después)

| Métrica | Antes | Después |
|---|---|---|
| Pruebas backend (unitarias + integración + contrato) | 38 | 38 |
| Pruebas E2E | 4 | 4 |
| Pruebas app | 19 | 19 |
| Cobertura de líneas del backend (JaCoCo) | 95 % (1025/1079) | 95,3 % (1028/1079) |
| Líneas de código: backend `src/main` | 4630 (22 archivos) | 4647 (25 archivos) |
| Líneas de código: app (sin pruebas) | 11248 (91 archivos) | 11240 (91 archivos) |
| Dependencias declaradas en Gradle (backend / app) | 27 / 30 | 26 / 29 |
| Advertencias del compilador (backend / app) | 15 / 8 | 12 / 8 |

El recuento inicial de «0/0 advertencias Kotlin» era inexacto; los valores reales se midieron sobre `v0.1.0` y sobre el árbol final con los mismos comandos (`--warning-mode all --rerun-tasks`; app también `--no-configuration-cache`). Los avisos restantes son preexistentes y están en los mismos archivos. Las 3 líneas nuevas en backend `src/main` son los archivos compartidos extraídos (`Pagination.kt`, `GroupCodes.kt`, `DateParsing.kt`); el aumento total se debe a helpers y nombres explícitos, no a lógica nueva.

## Dependencias de Gradle quitadas

- `testImplementation("io.mockk:mockk:1.14.12")` en `backend/build.gradle.kts` (0 imports `io.mockk`).
- En `Librerias_eduvia/gradle/libs.versions.toml`: `kotlin.testJunit`, `junit`, `androidx.core.ktx`, `androidx.testExt.junit`, `androidx.espresso.core` y `androidx.appcompat`, junto con sus `version.ref` huérfanos.
- Declaración redundante `implementation(libs.compose.foundation)` en `shared/build.gradle.kts`; se conserva `api("org.jetbrains.compose.foundation:foundation:1.12.1")`, así que la visibilidad del módulo no cambia.

No se cambió ninguna versión de las dependencias restantes ni se añadió ninguna librería.

## Verificación

- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-all.ps1`: **✅ 5/5** tras cada uno de los cuatro grupos de cambios, tras la limpieza de entorno y al final. Sin pruebas desactivadas ni rebajadas.
- **6.4** `git diff --stat v0.1.0 -- contract/fixtures backend/src/main/resources/db/migration`: salida vacía.
- **6.5** OpenAPI «antes» y «después»: 26 paths y 35 schemas; sin diferencias en rutas, métodos, parámetros, `operationId`, códigos de respuesta ni esquemas. Ambos archivos son byte-idénticos (SHA-256 `A2243A846A1EF392D387CF1A0C97B8A6D11C3D10BEC4B3A31DB7C10C257C70E0`). Se usó el puerto aislado 18080 porque 8080 está ocupado por un Apache ajeno.
- **6.6** Prueba real en emulador `Pixel_7` (`emulator-5554`): APK debug instalado, login con la cuenta demo, pantalla «Hola, Laura» con 6 tareas y 2 juntas, recorridos Inicio/Tareas/Calendario/Juntas con datos reales. `docker compose logs api`: 0 respuestas 4xx/5xx inesperadas (solo dos avisos de SpringDoc por tener los endpoints de documentación habilitados). Redirección temporal y stack aislado eliminados al terminar.
- `git status` limpio; HEAD local igual a `origin/refactor/limpieza` tras el push final.

## Descartados y pendientes

- **Descartado:** R-01 (responsabilidades) por no existir hallazgos; mover código habría sido un cambio 🔴 sin beneficio.
- **Revertidos:** ninguno. Todas las verificaciones pasaron a la primera.
- **8.1, conservados ante duda:** `RegisterTeacherViewModel`, `Meeting.hasVirtualLink` (campo del contrato en fixtures), starters `-test` de Spring Boot 4 y el `spring.application.name` duplicado que `docs/HALLAZGOS.md` H11 indica conservar.
- **8.2, propuestas que cambian comportamiento y no se aplicaron:** implementar copiar/compartir en `GroupsReadyScreen.kt`, y quitar la comprobación de 409 por código aleatorio en el registro.

## Pull Request

La guía pide PR a `main`, pero `gh` no está instalado en este equipo y el navegador no tiene sesión iniciada en GitHub, así que **no se pudo crear**. El PR debe abrirse manualmente con:

```bash
gh pr create --base main --head refactor/limpieza \
  --title "refactor: limpieza y simplificación del código" \
  --body-file docs/REFACTOR-RESUMEN.md
```

No se hizo merge: la revisión la hace el usuario.
