# Hallazgos — EduvIA

> Inventario de **bugs, inconsistencias y código confuso** detectados durante la documentación. Los hallazgos de código y los cambios en documentos preexistentes **no se han arreglado**: este archivo los describe con archivo, línea, evidencia y propuesta. Dos incidencias puramente documentales (H6 y H12) sí se aplicaron porque entraban en el alcance permitido de archivos `.md`, y están marcadas como **resuelta**. Si una corrección cambiaría el comportamiento, requiere autorización explícita.
>
> Los puntos que necesitan decisión también se reflejan en [`backend/OPEN-QUESTIONS.md`](backend/OPEN-QUESTIONS.md) con la marca `⚠️ Por confirmar`.

## Inconsistencias entre documentos existentes

### H1 — Nombre incorrecto del archivo de datos de ejemplo

- **Archivo y línea:** `docs/backend/data-model.md:212`.
- **Evidencia:** dice `V1000__seed_dev.sql`, pero el archivo real es `backend/src/main/resources/db/dev-data/V1000__seed.sql` (confirmado también en `docs/backend/PROGRESS.md:66`).
- **Propuesta:** corregir el nombre en `data-model.md` para que el enlace apunte al archivo real. Es un cambio de documentación, no de código.

### H2 — Fase 6 marcada como pendiente en el resumen

- **Archivo y línea:** `docs/backend/PROGRESS.md:12`.
- **Evidencia:** la casilla `- [ ] Fase 6` contradice el encabezado `Fase 6 — Prueba manual de punta a punta` con estado `✅ TERMINADA (con emulador real)` (línea 186) y la evidencia de las líneas 188–215.
- **Propuesta:** marcar la casilla `- [x]` y dejar el detalle de la reversión de `adb reverse` y del stack `miapp-manual` tal como está.

### H3 — Recuento de schemas distinto dentro del mismo informe

- **Archivo y líneas:** `docs/backend/compat-report.md:13` y `docs/backend/compat-report.md:70`.
- **Evidencia:** la tabla resumen afirma `35 schemas runtime`; la sección de evidencia afirma `26 paths / 34 operaciones / 36 schemas` tras añadir M9.
- **Propuesta:** unificar en **36 schemas** (valor verificado en la comprobación final de Fase 7) y aclarar que el de 35 corresponde al estado anterior a M9.

### H4 — Aritmética de operaciones del contrato

- **Archivos y líneas:** `docs/backend/api-contract.md:133` y `docs/backend/test-plan.md:112`.
- **Evidencia:** se afirma `8 + 7 + 5 + 8 + 3 = 31` operaciones y `31 endpoints × casos`; las filas reales del contrato son A1–A9 (9), G1–G7 (7), T1–T6 (6), M1–M9 (9) y C1–C3 (3) = **34**, cifra coincidente con `compat-report.md:12` y con `/v3/api-docs`.
- **Propuesta:** corregir el desglose a `9 + 7 + 6 + 9 + 3 = 34` en ambos documentos; la diferencia de 31 a 34 viene de los tres endpoints añadidos después del diseño inicial (A7/A8/A9 y M9 en revisiones posteriores).

### H5 — Casillas de pruebas pendientes pese al resultado en verde

- **Archivo y líneas:** `docs/backend/test-plan.md:13`, `:17`, `:23`, `:25`, `:27`, `:35`, `:39`, `:43`, `:44`, `:56`, `:58`, `:62`, `:69`, `:76` y otras.
- **Evidencia:** esos escenarios siguen con `[ ]`, mientras `docs/backend/PROGRESS.md:228-229` registra 38 tests backend y 19 tests KMP en verde y `scripts/test-all.ps1` termina correctamente.
- **Propuesta:** distinguir en el plan entre «escenario no automatizado» y «casilla no actualizada», y marcar solo los que tengan prueba real; los que no se ejecutaron deben quedar explícitamente como pendientes con motivo.

### H6 — README de la app describe carpetas y tareas que no existen aquí — ✅ RESUELTA

- **Archivo y líneas:** `Librerias_eduvia/README.md:12` y `:26`.
- **Evidencia:** menciona `jvmMain` (no presente en `Librerias_eduvia/shared/src/`) y recomienda `./gradlew :shared:testAndroidHostTest`, mientras `scripts/test-all.ps1:6` usa `:shared:testAndroid` y `PROGRESS.md:229` verifica esa tarea. Además, el README sigue siendo la plantilla genérica de KMP y no describe la capa de red Ktor.
- **Propuesta:** reescribir el README corto de la app con los módulos reales, la base URL por plataforma y el enlace a `docs/app-kmp.md`, conservando los comandos que sí funcionan.
- **Resolución:** se reescribió `Librerias_eduvia/README.md` con los source sets reales y los comandos verificados; el cambio es solo `.md`, sin tocar código.

### H13 — Nombre de la docente distinto en el fixture y en la semilla

- **Archivos y líneas:** `contract/fixtures/login-response.json:4` y `backend/src/main/resources/db/dev-data/V1000__seed.sql:6`.
- **Evidencia:** el fixture dice `Laura Martínez` y la base de datos sembrada guarda `Laura Hernández` para el mismo `id=1` y correo `laura@eduvia.mx`. Ambos valores se usan en pruebas y en la demo.
- **Propuesta:** decidir cuál es el nombre canónico y regenerar el fixture o ajustar la semilla; no se cambia aquí porque afectaría a datos de prueba y a la demo.

### H7 — La guía de trabajo usa nombres de la plantilla, no del repositorio

- **Archivo y líneas:** `GUIA-OPENCODE.md:69`, `:912` y `:1054` (entre otras).
- **Evidencia:** habla de `composeApp/`, de `scripts/test-all.sh` y de `:shared:testDebugUnitTest`; en este repositorio los nombres reales son `Librerias_eduvia/`, `scripts/test-all.ps1` y `:shared:testAndroid`.
- **Propuesta:** añadir una nota al inicio de la guía con las equivalencias de nombres de este proyecto, sin reescribir la guía (es el plan de trabajo original).

## Código confuso (sin corregir)

### H8 — Botones «copiar» y «compartir» sin acción

- **Archivo y líneas:** `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/auth/GroupsReadyScreen.kt:136` y `:142`.
- **Evidencia:** ambos `clickable { /* TODO copiar */ }` y `clickable { /* TODO compartir */ }` tienen cuerpo vacío; al pulsarlos no ocurre nada.
- **Propuesta:** si se quiere funcionalidad real, implementar portapapeles y compartir en las capas `expect/actual`; mientras tanto, documentar en `docs/app-kmp.md` que son affordances visuales de la pantalla de grupos.

### H9 — Grupo fijo `1` en el inicio de la demo

- **Archivo y línea:** `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/ui/home/HomeViewModel.kt:18`.
- **Evidencia:** comentario `// Observa las tareas (asumimos que el grupo 1 es el del usuario actual para esta demo)`; el repo remoto exige `groupId`, por lo que la pantalla no usa el grupo del usuario autenticado.
- **Propuesta original:** documentar la limitación y, si se decide corregir, pasar el `groupId` desde la sesión en lugar del literal `1`.
- **Resolución:** corregido; `AppNavHost` resuelve el grupo activo desde los grupos reales de la sesión y lo pasa a `HomeViewModel` y a `TasksViewModel`.

### H10 — Repositorios «fake» conviven con los remotos

- **Archivo y líneas:** `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/repository/` (5 implementaciones) y `Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/di/AppContainer.kt:17-35`.
- **Evidencia:** las implementaciones en memoria siguen existiendo y `AppContainer` inyecta las remotas; leer solo el paquete `data/repository` puede dar a entender que la app todavía no llama a la API.
- **Propuesta original:** explicar en `docs/app-kmp.md` y en el KDoc de `AppContainer` que son implementaciones de reserva/ejemplo y que la ruta activa es `data/remote`. No eliminarlas sin decisión.
- **Resolución:** eliminadas las 5 implementaciones en memoria y los 4 archivos de datos de muestra que las alimentaban; la app solo usa `data/remote`.

### H11 — `spring.application.name` duplicado

- **Archivos y líneas:** `backend/src/main/resources/application.properties:1` y `backend/src/main/resources/application.yml:2`.
- **Evidencia:** ambos definen el mismo valor `api`; es inofensivo pero confunde al buscar la configuración efectiva.
- **Propuesta:** conservar `application.yml` como fuente única y convertir `application.properties` en un archivo comentado o eliminarlo en una tarea aparte (cambio de configuración, no se hace aquí).

## Formato de documentación

### H12 — Diagrama ASCII en guías existentes — ✅ RESUELTA

- **Archivo y líneas:** `docs/guias/README.md:7-10` (y bloques `text` equivalentes en `docs/guias/api-rest.md`, `docs/guias/app-kotlin.md` y `docs/guias/base-de-datos.md`).
- **Evidencia:** los bloques con `──mensaje──▶`, el árbol de módulos y el ER en ASCII quedaban fuera de la regla «solo Mermaid» de esta tarea.
- **Propuesta:** sustituirlos por `flowchart`, `sequenceDiagram` y `erDiagram` de Mermaid con etiquetas entre comillas. Es cambio de `.md`, por lo que sí se aplica en la fase de documentación del proyecto.
- **Resolución:** los cuatro archivos se convirtieron a Mermaid y los 21 bloques originales se validaron entonces con el parser de `@mermaid-js/mermaid-cli` 11.17.1 (el repositorio tiene más bloques añadidos después).
