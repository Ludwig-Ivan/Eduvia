package com.example.librerias.api.web

import com.example.librerias.api.service.TaskService
import com.example.librerias.api.web.dto.CreateTaskRequest
import com.example.librerias.api.web.dto.MarkDeliveredRequest
import com.example.librerias.api.web.dto.UpdateTaskRequest
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
 * Capa controller de tareas escolares (T1-T6), que refleja las operaciones de `TaskRepository` de la app KMP.
 *
 * Colabora con [TaskService] para listar por grupo, consultar detalle, crear, editar, borrar y marcar entregas. El servicio mantiene sincronizado el calendario en la misma transacción: el evento de una tarea comparte su id, a diferencia del de las juntas, que usa `1000 + id`. El estado `PENDING`/`DELIVERED`/`EXPIRED` y las entregas son datos derivados que el controlador expone ya calculados. Los listados aplican `size <= 100` y orden por fecha de vencimiento e id. La cadena de seguridad responde 401 o 403 antes de este controlador; los errores de dominio se convierten en `ProblemDetail` (RFC 9457) en [GlobalExceptionHandler].
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(
    name = "Tareas",
    description = "Tareas escolares: listado por grupo, alta y edición sincronizadas con el calendario, borrado y marcas de entrega idempotentes.",
)
class TaskController(private val service: TaskService) {

    /**
     * Lista las tareas de un grupo en páginas ordenadas por fecha de vencimiento e id.
     *
     * Capa controller: construye el `PageRequest` con orden determinista y delega la consulta en [TaskService]; el límite `size <= 100` es el acordado para las respuestas paginadas de la app KMP.
     *
     * @param groupId identificador del grupo cuyas tareas se consultan.
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `SchoolTaskDto` (contrato T1).
     * @throws ApiException con 404 si el grupo no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si algún parámetro no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping
    @Operation(
        summary = "Listar tareas de un grupo",
        description = "Página de tareas ordenada por fecha de vencimiento e id, con los ids de alumnos que ya la entregaron.",
    )
    @ApiResponse(responseCode = "200", description = "Página de tareas del grupo.")
    @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun list(
        @Parameter(description = "Id del grupo cuyas tareas se consultan.", example = "1")
        @RequestParam groupId: Long,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.list(groupId, PageRequest.of(page, size.coerceAtMost(MAX_PAGE_SIZE), Sort.by("dueDate", "id")))

    /**
     * Devuelve el detalle de una tarea con el estado calculado a partir de las entregas y la fecha de vencimiento.
     *
     * Capa controller: delega la búsqueda y el cálculo del estado en [TaskService].
     *
     * @param id identificador de la tarea.
     * @return `200 OK` con el `SchoolTaskDto` (contrato T2).
     * @throws ApiException con 404 si la tarea no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Detalle de tarea",
        description = "Tarea con `status` derivado (`PENDING`, `DELIVERED` o `EXPIRED`) y la lista de entregas para la app KMP.",
    )
    @ApiResponse(responseCode = "200", description = "Tarea encontrada.")
    @ApiResponse(responseCode = "404", description = "Tarea no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun get(
        @Parameter(description = "Id de la tarea.", example = "1")
        @PathVariable id: Long,
    ) = service.get(id)

    /**
     * Crea una tarea y su evento de calendario en la misma transacción, compartiendo ambos el mismo id.
     *
     * Capa controller: `@Valid` comprueba el DTO y [TaskService] valida título y fecha, recupera el docente del grupo y persiste el evento de tipo `TASK`; el id compartido permite a la app KMP correlacionar tarea y calendario sin conversiones adicionales.
     *
     * @param body datos validados de la tarea: grupo, título, materia, descripción y fecha en texto `YYYY-MM-DD`.
     * @return `201 Created` con el `SchoolTaskDto` creado (contrato T3).
     * @throws ApiException con 400 si título o fecha son inválidos; con 404 si el grupo o el docente no existen. Se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping
    @Operation(
        summary = "Crear tarea",
        description = "Registra la tarea y su evento de calendario con el mismo id en una sola transacción.",
    )
    @ApiResponse(responseCode = "201", description = "Tarea y evento de calendario creados.")
    @ApiResponse(responseCode = "400", description = "Título o fecha inválidos.")
    @ApiResponse(responseCode = "404", description = "Grupo o docente no encontrados.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun create(@Valid @RequestBody body: CreateTaskRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(body.groupId, body.title, body.subject, body.description, body.dateText))

    /**
     * Actualiza una tarea y propaga el cambio de fecha, título y materia a su evento de calendario.
     *
     * Capa controller: valida el DTO y delega en [TaskService] la edición y la sincronización del evento con el mismo id.
     *
     * @param id identificador de la tarea que se edita.
     * @param body nuevos valores validados: título, materia, descripción y fecha en texto `YYYY-MM-DD`.
     * @return `200 OK` con el `SchoolTaskDto` actualizado (contrato T4).
     * @throws ApiException con 400 si título o fecha son inválidos; con 404 si la tarea no existe. Se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar tarea",
        description = "Edita la tarea y su evento de calendario con el mismo id para mantener coherente la vista de la app KMP.",
    )
    @ApiResponse(responseCode = "200", description = "Tarea actualizada.")
    @ApiResponse(responseCode = "400", description = "Título o fecha inválidos.")
    @ApiResponse(responseCode = "404", description = "Tarea no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun update(
        @Parameter(description = "Id de la tarea que se edita.", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody body: UpdateTaskRequest,
    ) = service.update(id, body.title, body.subject, body.description, body.dateText)

    /**
     * Elimina una tarea junto con sus entregas y su evento de calendario.
     *
     * Capa controller: delega la eliminación en cascada en [TaskService]; la respuesta no incluye cuerpo porque la app KMP solo necesita confirmación.
     *
     * @param id identificador de la tarea.
     * @return `204 No Content` (contrato T5).
     * @throws ApiException con 404 si la tarea no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar tarea",
        description = "Borra la tarea, sus marcas de entrega y el evento de calendario que comparte su id.",
    )
    @ApiResponse(responseCode = "204", description = "Tarea eliminada.")
    @ApiResponse(responseCode = "404", description = "Tarea no encontrada.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun delete(
        @Parameter(description = "Id de la tarea.", example = "1")
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Marca una tarea como entregada por un alumno, de forma idempotente.
     *
     * Capa controller: valida el DTO y delega en [TaskService] la comprobación de pertenencia al grupo y la inserción solo si aún no existía la entrega; la respuesta devuelve la tarea con `deliveredStudentIds` actualizado.
     *
     * @param id identificador de la tarea entregada.
     * @param body identificador validado del alumno que entrega.
     * @return `200 OK` con el `SchoolTaskDto` y su lista de entregas (contrato T6).
     * @throws ApiException con 404 si la tarea o el alumno no existen; con 409 si el alumno no pertenece al grupo de la tarea. Se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/{id}/deliveries")
    @Operation(
        summary = "Marcar tarea como entregada",
        description = "Registra la entrega de un alumno del grupo sin duplicarla y devuelve la tarea con todos los ids entregados.",
    )
    @ApiResponse(responseCode = "200", description = "Entrega registrada; marca repetida sin efecto adicional.")
    @ApiResponse(responseCode = "400", description = "Identificador de alumno inválido.")
    @ApiResponse(responseCode = "404", description = "Tarea o alumno no encontrados.")
    @ApiResponse(responseCode = "409", description = "El alumno no pertenece al grupo de la tarea.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun markDelivered(
        @Parameter(description = "Id de la tarea entregada.", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody body: MarkDeliveredRequest,
    ) = service.markDelivered(id, body.studentId)
}
