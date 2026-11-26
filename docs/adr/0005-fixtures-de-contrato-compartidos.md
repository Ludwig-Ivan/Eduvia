# ADR 0005 — Fixtures de contrato compartidos entre backend y app

## Estado

Aceptada.

## Contexto

El contrato lo define la app KMP, que originalmente usaba repositorios en memoria (caso B). Tras la Fase 5, la app consume la API con Ktor **3.2.1** y `minSdk=24` (decisión 11: la 3.2.0 rompía D8 con `Messages`/`use streaming syntax`). La decisión 10 de [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md) indica que `RegisterUseCase` envía `children` y `groups` vacíos y la vinculación se hace después con endpoints separados; la decisión 12 añadió `PUT /meetings/{id}` (M9) para editar juntas conservando el id, excluyendo la propia junta del chequeo de duplicados y actualizando el evento `1000 + id`.

Hacía falta una prueba que detectara cambios de forma en ambos lados: no basta con que el backend pueda serializar un DTO si la app deja de poder leerlo.

## Decisión

Guardar **11 archivos JSON** reales en [`contract/fixtures/`](../../contract/fixtures/) (login, register, grupos, alumnos, tareas, juntas, calendario, asistencia y error de validación) y usarlos desde ambos lados:

- El backend pasa la ruta de esos archivos a sus tareas de prueba mediante la propiedad `contract.fixtures.dir`, definida en [`backend/build.gradle.kts`](../../backend/build.gradle.kts); las respuestas quedaron registradas en `contract/fixtures/` durante la verificación del contrato.
- La app los lee en [`ContractFixturesFileTest.kt`](../../Librerias_eduvia/shared/src/androidHostTest/kotlin/com/example/librerias/contract/ContractFixturesFileTest.kt) con un `Json` strict: **`ignoreUnknownKeys = false`**. Así, un campo nuevo o renombrado falla la prueba en vez de descartarse en silencio.

Los DTOs de ambos lados son espejo: IDs `Long`, camelCase, `dateText` `YYYY-MM-DD`, `parentId = 0` en la app ↔ `NULL` en BD, y offset 1000 para el evento de una junta. El informe [`docs/backend/compat-report.md`](../backend/compat-report.md) registra rutas, DTOs, nulabilidad, enums, fechas, errores, paginación y autenticación sin incompatibilidades.

El porqué: los fixtures son ejecutables y viajan con el repo; un `openapi.yaml` describe la intención, pero solo una respuesta real verificada por la app demuestra que el contrato sigue cumpliéndose.

## Consecuencias

### Positivas

- Un cambio de forma en el backend rompe la prueba de contrato; un DTO de la app que deja de coincidir rompe `ContractFixturesFileTest`. El avance de M9 se probó exactamente así, con pruebas rojas primero y verdes después.
- `ignoreUnknownKeys = false` convierte los campos sobrantes en una señal de alerta, algo que `ignoreUnknownKeys = true` en el cliente de producción no haría.
- La comparación `/v3/api-docs` contra [`docs/backend/openapi.yaml`](../backend/openapi.yaml) da `missing=[]` y `extra=[]`; la edición de juntas quedó documentada en M9, en el contrato y en el informe.
- Ktor 3.2.1 con `minSdk=24` permite dexar el APK y ejecutar las pruebas KMP en Android sin el fallo de D8 de la 3.2.0.

### Negativas

- Hay que actualizar los fixtures de forma intencionada (`-PupdateContract=true`) y adaptar la app en el mismo cambio; no se pueden regenerar a ciegas.
- La prueba estricta rechaza incluso campos de conveniencia nuevos que la app no consuma, lo que exige decidir si se añaden al DTO o se retiran del backend.
- ⚠️ Por confirmar: las implementaciones en memoria de `data/repository/*Impl.kt` siguen en el código; `AppContainer` inyecta los repositorios remotos (H10). ¿Conservarlas como ejemplo o retirarlas? Ver [`docs/backend/OPEN-QUESTIONS.md`](../backend/OPEN-QUESTIONS.md) Q12 y [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H10.
- Hallazgo documentado y sin corregir: el resumen de `compat-report.md` dice 35 schemas y la evidencia final dice 36, tras M9 (H3). Ver [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H3.

## Alternativas consideradas

- **Confiar solo en `openapi.yaml`:** descartada; no comprueba la serialización real ni lo que la app deserializa.
- **Mocks separados en backend y app:** descartada; cada lado podría seguir fallando sin que el otro lo detecte.
- **`ignoreUnknownKeys = true` en el test:** descartada; ocultaría precisamente los cambios de forma que se quieren detectar (el cliente de producción sí tolera campos desconocidos).
- **Un único fixture gigante:** descartada; 11 archivos permiten localizar qué respuesta cambió.

## Enlaces

- Decisiones: [`docs/backend/DECISIONS.md`](../backend/DECISIONS.md), filas 7, 8, 10, 11 y 12.
- Contrato e informe: [`docs/backend/api-contract.md`](../backend/api-contract.md), [`docs/backend/openapi.yaml`](../backend/openapi.yaml), [`docs/backend/compat-report.md`](../backend/compat-report.md).
- Fixtures: [`contract/fixtures/`](../../contract/fixtures/).
- App: [`ApiDtos.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/ApiDtos.kt), [`Mappers.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/Mappers.kt), [`RemoteMeetingRepository.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/data/remote/RemoteMeetingRepository.kt), [`AppContainer.kt`](../../Librerias_eduvia/shared/src/commonMain/kotlin/com/example/librerias/di/AppContainer.kt), [`ContractFixturesFileTest.kt`](../../Librerias_eduvia/shared/src/androidHostTest/kotlin/com/example/librerias/contract/ContractFixturesFileTest.kt).
- Backend: [`MeetingService.kt`](../../backend/src/main/kotlin/com/example/librerias/api/service/MeetingService.kt), [`MeetingIntegrationTest.kt`](../../backend/src/test/kotlin/com/example/librerias/api/MeetingIntegrationTest.kt).
- Dudas y hallazgos: [`docs/backend/OPEN-QUESTIONS.md`](../backend/OPEN-QUESTIONS.md), [`docs/HALLAZGOS.md`](../HALLAZGOS.md) H3 y H10.
