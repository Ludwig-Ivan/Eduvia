package com.example.librerias.api.web

import com.example.librerias.api.service.MeetingService
import com.example.librerias.api.web.dto.CreateMeetingRequest
import com.example.librerias.api.web.dto.SetResponseRequest
import com.example.librerias.api.web.dto.UpdateMeetingRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Capa controller de juntas (M1-M9), que refleja las operaciones de `MeetingRepository` de la app KMP.
 *
 * Colabora con [MeetingService] para listar, consultar, crear, editar y borrar juntas, así como para registrar respuestas, derivar la asistencia y contar pendientes. La regla de negocio de unicidad (grupo + fecha + hora + lugar) se resuelve en el servicio y se traduce a 409; el evento de calendario asociado usa siempre `1000 + meetings.id` porque en la app las juntas y las tareas tenían ids solapados. Los listados aplican `size <= 100` y orden estable por fecha e id. La cadena de seguridad responde 401 o 403 antes de este controlador; los errores de dominio se convierten en `ProblemDetail` (RFC 9457) en [GlobalExceptionHandler].
 */
@RestController
@RequestMapping("/api/v1/meetings")
@Tag(
    name = "Juntas",
    description = "Juntas escolares: alta, edición, borrado, respuestas de familias, asistencia derivada y recordatorios, con evento de calendario id 1000 + junta.",
)
class MeetingController(private val service: MeetingService) {

    /**
     * Lista juntas en páginas ordenadas por fecha e id, con la respuesta y los campos derivados ya calculados.
     *
     * Capa controller: delega en [MeetingService] la paginación y el mapeo; `size.coerceAtMost(100)` respeta el límite del contrato con la app móvil, que no pagina por sí misma.
     *
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `MeetingDto` (contrato M1).
     * @throws `MethodArgumentTypeMismatchException` si `page` o `size` no son numéricos; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping
    @Operation(
        summary = "Listar juntas",
        description = "Página de juntas ordenada por fecha e id, con `isNext` y `relativeLabel` calculados para la app KMP.",
    )
    @ApiResponse(responseCode = "200", description = "Página de juntas.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun list(
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.list(PageRequest.of(page, size.coerceAtMost(100), Sort.by("date", "id")))

    /**
     * Devuelve el mapa de respuestas de un alumno indexado por id de junta, para que la app KMP pinte el estado de cada junta sin consultarlas una a una.
     *
     * Capa controller: delega en [MeetingService], que usa como claves los ids de junta en formato texto por exigencia del JSON del contrato M7.
     *
     * @param studentId identificador del alumno cuyas respuestas se consultan.
     * @return `200 OK` con `ResponsesDto` (por ejemplo, `{"1":"ATTENDING"}`).
     * @throws `MethodArgumentTypeMismatchException` si `studentId` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/responses")
    @Operation(
        summary = "Respuestas de un alumno",
        description = "Mapa de id de junta a `MeetingResponse` para que la app KMP conozca de una sola llamada el estado de cada junta.",
    )
    @ApiResponse(responseCode = "200", description = "Mapa de respuestas por junta.")
    @ApiResponse(responseCode = "400", description = "Identificador de alumno inválido.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun responses(
        @Parameter(description = "Id del alumno cuyas respuestas se consultan.", example = "1")
        @RequestParam studentId: Long,
    ) = service.responsesOf(studentId)

    /**
     * Devuelve el detalle de una junta, incluida la respuesta agregada y los campos derivados que espera la app KMP.
     *
     * Capa controller: delega la búsqueda y el mapeo en [MeetingService].
     *
     * @param id identificador de la junta.
     * @return `200 OK` con el `MeetingDto` (contrato M2).
     * @throws ApiException con 404 si la junta no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Detalle de junta",
        description = "Junta identificada por su id con la respuesta calculada a partir de las familias que confirmaron asistencia.",
    )
    @ApiResponse(responseCode = "200", description = "Junta encontrada.")
    @ApiResponse(responseCode = "404", description = "Junta no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun get(
        @Parameter(description = "Id de la junta.", example = "1")
        @PathVariable id: Long,
    ) = service.get(id)

    /**
     * Crea una junta y, en la misma transacción, su evento de calendario con id `1000 + junta.id`.
     *
     * Capa controller: `@Valid` comprueba el DTO y [MeetingService] normaliza fecha, hora y lugar, aplica la unicidad de junta y genera el evento para la vista unificada del calendario; el desplazamiento de 1000 evita colisiones con los eventos de tarea, que usan el mismo id que la tarea.
     *
     * @param body datos validados de la junta: título, fecha en texto, hora, lugar, enlace opcional, recordatorio y grupo.
     * @return `201 Created` con el `MeetingDto` creado (contrato M3).
     * @throws ApiException con 400 si título, fecha, hora, lugar o URL son inválidos; con 404 si el grupo no existe; con 409 si ya hay una junta en la misma fecha, hora, lugar y grupo. Se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping
    @Operation(
        summary = "Crear junta",
        description = "Registra la junta y su evento de calendario `1000 + id` en una transacción, rechazando duplicados de fecha, hora, lugar y grupo.",
    )
    @ApiResponse(responseCode = "201", description = "Junta y evento de calendario creados.")
    @ApiResponse(responseCode = "400", description = "Título, fecha, hora, lugar o enlace inválidos.")
    @ApiResponse(responseCode = "409", description = "Ya existe una junta en esa fecha, hora, lugar y grupo.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun create(@Valid @RequestBody body: CreateMeetingRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            service.create(body.title, body.dateText, body.time, body.place, body.link, body.reminder, body.groupId),
        )

    /**
     * Actualiza una junta y sincroniza su evento de calendario `1000 + id`, excluyendo la propia junta de la comprobación de duplicados.
     *
     * Capa controller: valida el DTO y delega la normalización, la regla de unicidad y la actualización del evento en [MeetingService]; la respuesta incluye la junta ya mapeada para la app KMP.
     *
     * @param id identificador de la junta que se edita.
     * @param body nuevos valores validados: título, fecha, hora, lugar, enlace opcional y recordatorio.
     * @return `200 OK` con el `MeetingDto` actualizado (contrato M9).
     * @throws ApiException con 400 si título, fecha, hora, lugar o URL son inválidos; con 404 si la junta no existe; con 409 si otra junta ocupa la misma fecha, hora, lugar y grupo. Se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar junta",
        description = "Edita la junta y su evento `1000 + id`, manteniendo la unicidad de fecha, hora, lugar y grupo sin contarse a sí misma.",
    )
    @ApiResponse(responseCode = "200", description = "Junta actualizada.")
    @ApiResponse(responseCode = "400", description = "Título, fecha, hora, lugar o enlace inválidos.")
    @ApiResponse(responseCode = "404", description = "Junta no encontrada.")
    @ApiResponse(responseCode = "409", description = "Otra junta ocupa esa fecha, hora, lugar y grupo.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun update(
        @Parameter(description = "Id de la junta que se edita.", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody body: UpdateMeetingRequest,
    ) = service.update(id, body.title, body.dateText, body.time, body.place, body.link, body.reminder)

    /**
     * Borra una junta junto con sus respuestas y su evento de calendario `1000 + id`.
     *
     * Capa controller: delega la eliminación en cascada en [MeetingService]; la respuesta no incluye cuerpo porque la app KMP solo necesita confirmación.
     *
     * @param id identificador de la junta.
     * @return `204 No Content` (contrato M4).
     * @throws ApiException con 404 si la junta no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar junta",
        description = "Elimina la junta, sus respuestas y el evento de calendario asociado `1000 + id`.",
    )
    @ApiResponse(responseCode = "204", description = "Junta eliminada.")
    @ApiResponse(responseCode = "404", description = "Junta no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun delete(
        @Parameter(description = "Id de la junta.", example = "1")
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Lista en páginas las respuestas de las familias de una junta, con el resumen de asistencia derivado de esas respuestas.
     *
     * Capa controller: delega en [MeetingService]; la asistencia no se almacena, se calcula a partir de los estados `ATTENDING`, `DECLINED` y `PENDING`, y el tamaño de página se limita a 100.
     *
     * @param id identificador de la junta.
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `FamilyResponseDto` (contrato M5).
     * @throws ApiException con 404 si la junta o un alumno asociado no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si algún parámetro no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/{id}/attendance")
    @Operation(
        summary = "Asistencia a una junta",
        description = "Página de respuestas familiares; el resumen de asistencia se deriva de ellas y nunca se almacena.",
    )
    @ApiResponse(responseCode = "200", description = "Página de respuestas de las familias.")
    @ApiResponse(responseCode = "404", description = "Junta no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun attendance(
        @Parameter(description = "Id de la junta.", example = "1")
        @PathVariable id: Long,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.attendance(id, PageRequest.of(page, size.coerceAtMost(100)))

    /**
     * Registra o actualiza la respuesta de un alumno a una junta, recalculando la respuesta agregada que ve la app KMP.
     *
     * Capa controller: valida el DTO y delega la escritura en [MeetingService]; la operación es idempotente en el sentido de que reutiliza la fila existente en lugar de duplicarla.
     *
     * @param id identificador de la junta.
     * @param studentId identificador del alumno que responde.
     * @param body respuesta validada (`PENDING`, `ATTENDING` o `DECLINED`).
     * @return `200 OK` con el `MeetingDto` actualizado (contrato M6).
     * @throws ApiException con 404 si la junta o el alumno no existen; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo o el enum de respuesta; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws `MethodArgumentTypeMismatchException` si los ids de la ruta no son numéricos; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PutMapping("/{id}/responses/{studentId}")
    @Operation(
        summary = "Registrar respuesta de una familia",
        description = "Crea o actualiza la respuesta del alumno y devuelve la junta con el estado agregado recalculado.",
    )
    @ApiResponse(responseCode = "200", description = "Respuesta registrada y junta actualizada.")
    @ApiResponse(responseCode = "400", description = "Respuesta no válida o ids incorrectos.")
    @ApiResponse(responseCode = "404", description = "Junta o alumno no encontrados.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun setResponse(
        @Parameter(description = "Id de la junta.", example = "1")
        @PathVariable id: Long,
        @Parameter(description = "Id del alumno que responde.", example = "1")
        @PathVariable studentId: Long,
        @Valid @RequestBody body: SetResponseRequest,
    ) = service.setResponse(id, studentId, body.response)

    /**
     * Devuelve cuántas familias siguen sin responder a una junta, para calcular los recordatorios pendientes.
     *
     * Capa controller: delega el recuento en [MeetingService], que cuenta las respuestas `PENDING` derivadas de la tabla de respuestas y no emite notificaciones reales.
     *
     * @param id identificador de la junta.
     * @return `200 OK` con `PendingDto` (contrato M8).
     * @throws ApiException con 404 si la junta no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/{id}/reminders")
    @Operation(
        summary = "Contar recordatorios pendientes",
        description = "Devuelve el número de respuestas PENDING de la junta, usado por la app KMP para mostrar recordatorios.",
    )
    @ApiResponse(responseCode = "200", description = "Número de respuestas pendientes.")
    @ApiResponse(responseCode = "404", description = "Junta no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun remind(
        @Parameter(description = "Id de la junta.", example = "1")
        @PathVariable id: Long,
    ) = service.remindPending(id)
}
