package com.example.librerias.api.web

import com.example.librerias.api.domain.UserRole
import com.example.librerias.api.service.CalendarService
import com.example.librerias.api.web.dto.CreateCalendarEventRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Capa controller del calendario del contrato C1-C3, que la app KMP consume como vista unificada de tareas y juntas.
 *
 * Colabora con [CalendarService] para listar, crear y borrar eventos. El servicio deriva la asistencia solo para el rol docente, porque la vista del padre no la muestra. Los eventos de tarea comparten el id de la tarea y los de junta usan la regla `1000 + meetings.id` para evitar que los ids de la app, que antes se solapaban, colisionen en una sola tabla. El tamaño de página se limita a 100 para cumplir el contrato de paginación. La cadena de seguridad responde 401 o 403 antes de este controlador; los errores de dominio se convierten en `ProblemDetail` (RFC 9457) en [GlobalExceptionHandler].
 */
@RestController
@RequestMapping("/api/v1/calendar/events")
@Tag(
    name = "Calendario",
    description = "Eventos unificados de tareas y juntas: listado por rol, alta directa y borrado, con la vista de asistencia reservada al docente.",
)
class CalendarController(private val service: CalendarService) {

    /**
     * Lista los eventos del calendario en páginas, con asistencia calculada solo para el rol docente.
     *
     * Capa controller: convierte `page` y `size` en un `PageRequest` y delega el filtrado y el mapeo en [CalendarService]; `size.coerceAtMost(100)` aplica el límite de paginación acordado con la app KMP, que no pagina por sí misma.
     *
     * @param role rol del usuario que consulta: `PARENT` oculta la asistencia y `TEACHER` la incluye en los eventos de junta.
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `CalendarEventDto` (contrato C1).
     * @throws ApiException con 400 si el rol no es un literal admitido; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `role`, `page` o `size` no se pueden convertir; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping
    @Operation(
        summary = "Listar eventos del calendario",
        description = "Devuelve una página ordenada por fecha; el docente recibe la asistencia derivada de las juntas y el padre no.",
    )
    @ApiResponse(responseCode = "200", description = "Página de eventos del calendario.")
    @ApiResponse(responseCode = "400", description = "Rol no admitido o parámetros de paginación inválidos.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun list(
        @Parameter(description = "Rol que determina la vista: PARENT oculta la asistencia; TEACHER la incluye en los eventos de junta.", example = "PARENT")
        @RequestParam role: UserRole,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.list(role, PageRequest.of(page, size.coerceAtMost(100)))

    /**
     * Crea un evento directo en el calendario, sin depender de una tarea o junta previa.
     *
     * Capa controller: `@Valid` comprueba el DTO y [CalendarService] verifica que el grupo exista y persiste el evento; las altas de tareas y juntas crean sus propios eventos desde sus servicios, con ids `tarea.id` y `1000 + junta.id` respectivamente para no solapar los ids de la app KMP.
     *
     * @param body datos validados del evento: grupo, fecha, tipo, título y subtítulo.
     * @return `201 Created` con el `CalendarEventDto` creado (contrato C2).
     * @throws ApiException con 404 si el grupo indicado no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo o el tipo/fecha no son válidos; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping
    @Operation(
        summary = "Crear evento de calendario",
        description = "Añade un evento a un grupo existente; las altas de tareas y juntas generan sus eventos de forma equivalente con ids no solapados.",
    )
    @ApiResponse(responseCode = "201", description = "Evento creado.")
    @ApiResponse(responseCode = "400", description = "Datos del evento inválidos, incluida la fecha.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun create(@Valid @RequestBody body: CreateCalendarEventRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            service.create(body.groupId, body.date, body.type, body.title, body.subtitle),
        )

    /**
     * Elimina un evento del calendario por su id, que puede corresponder a una tarea o a `1000 + id` de una junta.
     *
     * Capa controller: delega la comprobación de existencia y el borrado en [CalendarService]. Cuando se borra la tarea o la junta origen, su servicio elimina también este evento para mantener coherente la vista de la app KMP.
     *
     * @param id identificador del evento; los eventos de junta usan el desplazamiento de 1000 sobre el id de la junta.
     * @return `204 No Content` (contrato C3).
     * @throws ApiException con 404 si el evento no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar evento de calendario",
        description = "Quita el evento de la vista unificada; el borrado de su tarea o junta origen también lo elimina.",
    )
    @ApiResponse(responseCode = "204", description = "Evento eliminado.")
    @ApiResponse(responseCode = "404", description = "Evento no encontrado.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun delete(
        @Parameter(description = "Id del evento; para juntas equivale a 1000 más el id de la junta.", example = "1001")
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
