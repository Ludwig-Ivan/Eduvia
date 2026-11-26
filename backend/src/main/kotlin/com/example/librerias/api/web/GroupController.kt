package com.example.librerias.api.web

import com.example.librerias.api.service.GroupService
import com.example.librerias.api.web.dto.CreateGroupRequest
import com.example.librerias.api.web.dto.LinkStudentRequest
import com.example.librerias.api.web.dto.toDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Capa controller de grupos y alumnos (G1-G7), que reproduce las operaciones de `GroupRepository` de la app KMP.
 *
 * Colabora con [GroupService] para consultar grupos por profesor, id o código de clase, crear grupos con código `XX-YYYY` único y vincular alumnos actualizando el contador del grupo. Los listados usan paginación con `size <= 100` y orden estable por id. La app representa "sin tutor" con `parentId = 0`, mientras la base de datos guarda `NULL`; el mapeo se resuelve en la capa de DTO, no aquí. La cadena de seguridad responde 401 o 403 antes de este controlador; los errores de dominio se convierten en `ProblemDetail` (RFC 9457) en [GlobalExceptionHandler].
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Grupos y alumnos",
    description = "Grupos escolares y vinculación de alumnos: búsqueda por docente, id o código de clase, lista de alumnos y alta de grupos.",
)
class GroupController(private val service: GroupService) {

    /**
     * Lista los grupos de un docente en páginas ordenadas por id.
     *
     * Capa controller: construye el `PageRequest` con orden determinista y delega la consulta en [GroupService]; el límite `size <= 100` evita respuestas excesivas para una app móvil.
     *
     * @param teacherId id del docente propietario de los grupos.
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `SchoolGroupDto` (contrato G1).
     * @throws `MethodArgumentTypeMismatchException` si `teacherId`, `page` o `size` no son numéricos; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/groups")
    @Operation(
        summary = "Listar grupos de un docente",
        description = "Página ordenada por id de los grupos creados por el docente indicado, con tamaño máximo de 100.",
    )
    @ApiResponse(responseCode = "200", description = "Página de grupos del docente.")
    @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun list(
        @Parameter(description = "Id del docente cuyos grupos se consultan.", example = "1")
        @RequestParam teacherId: Long,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.listByTeacher(teacherId, PageRequest.of(page, size.coerceAtMost(100), Sort.by("id")))

    /**
     * Busca un grupo por su código de clase, que la app KMP introduce para vincular alumnos.
     *
     * Capa controller: delega la búsqueda insensible a mayúsculas en [GroupService], que devuelve el grupo o señala que el código no existe.
     *
     * @param code código `XX-YYYY` del grupo, recibido en la query.
     * @return `200 OK` con el `SchoolGroupDto` encontrado (contrato G3).
     * @throws ApiException con 404 si ningún grupo tiene ese código; se convierte en `ProblemDetail` con ese código.
     * @throws Exception si el enlazado de la query falla (400) o si ocurre un error no controlado (500).
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     */
    @GetMapping("/groups/by-code")
    @Operation(
        summary = "Buscar grupo por código",
        description = "Localiza el grupo asociado a un código de clase, ignorando mayúsculas y espacios periféricos.",
    )
    @ApiResponse(responseCode = "200", description = "Grupo encontrado.")
    @ApiResponse(responseCode = "404", description = "Código de clase no encontrado.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun byCode(
        @Parameter(description = "Código de clase con formato XX-YYYY; la búsqueda ignora mayúsculas.", example = "AB-1234")
        @RequestParam code: String,
    ) = service.getByCode(code)

    /**
     * Devuelve el detalle de un grupo por su identificador.
     *
     * Capa controller: delega la búsqueda y el mapeo a DTO en [GroupService].
     *
     * @param id identificador del grupo.
     * @return `200 OK` con el `SchoolGroupDto` (contrato G2).
     * @throws ApiException con 404 si el grupo no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si `id` no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/groups/{id}")
    @Operation(
        summary = "Detalle de grupo",
        description = "Datos del grupo identificado por su id, incluidos el código de clase y el número de alumnos.",
    )
    @ApiResponse(responseCode = "200", description = "Grupo encontrado.")
    @ApiResponse(responseCode = "404", description = "Grupo no encontrado.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun get(
        @Parameter(description = "Id del grupo.", example = "1")
        @PathVariable id: Long,
    ) = service.get(id)

    /**
     * Crea un grupo para un docente y le asigna un código de clase único generado por el servicio.
     *
     * Capa controller: valida el DTO y delega en [GroupService] la creación y la generación de un código `XX-YYYY` con alfabeto sin ambigüedades; la respuesta 201 incluye el grupo ya mapeado para que la app KMP pueda mostrarlo sin recargar.
     *
     * @param body datos validados del grupo: docente, nombre, colegio y materia.
     * @return `201 Created` con el `SchoolGroupDto` creado (contrato G4).
     * @throws ApiException con 400 si nombre o colegio están vacíos; con 409 si no se puede generar un código único; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/groups")
    @Operation(
        summary = "Crear grupo",
        description = "Registra un grupo con código de clase único que la app KMP usará para vincular alumnos.",
    )
    @ApiResponse(responseCode = "201", description = "Grupo creado con su código de clase.")
    @ApiResponse(responseCode = "400", description = "Nombre o colegio obligatorios.")
    @ApiResponse(responseCode = "409", description = "No se pudo generar un código de grupo único.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun create(@Valid @RequestBody body: CreateGroupRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(body.teacherId, body.groupName, body.schoolName, body.subject).toDto())

    /**
     * Lista en páginas los alumnos vinculados a un grupo, ordenados por id.
     *
     * Capa controller: delega en [GroupService], que comprueba que el grupo exista antes de consultar; mantiene el límite `size <= 100` de la paginación del contrato.
     *
     * @param id identificador del grupo cuyos alumnos se consultan.
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `StudentDto` (contrato G5).
     * @throws ApiException con 404 si el grupo no existe; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentTypeMismatchException` si algún parámetro no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/groups/{id}/students")
    @Operation(
        summary = "Alumnos de un grupo",
        description = "Página de alumnos del grupo; cada `StudentDto` expone `parentId = 0` cuando el alumno no tiene tutor en la app KMP.",
    )
    @ApiResponse(responseCode = "200", description = "Página de alumnos del grupo.")
    @ApiResponse(responseCode = "404", description = "Grupo no encontrado.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun studentsOfGroup(
        @Parameter(description = "Id del grupo.", example = "1")
        @PathVariable id: Long,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.studentsOfGroup(id, PageRequest.of(page, size.coerceAtMost(100), Sort.by("id")))

    /**
     * Lista en páginas los alumnos asociados a un tutor, ordenados por id.
     *
     * Capa controller: delega la consulta en [GroupService]; el valor `parentId = 0` que envía la app KMP para "sin tutor" se interpreta en el mapeo como `NULL` en la base de datos, de modo que la consulta solo encuentra alumnos con tutor real.
     *
     * @param parentId id del tutor; `0` es la representación de la app para ausencia de tutor (`NULL` en BD).
     * @param page índice de página basado en 0.
     * @param size tamaño solicitado; se limita a 100 elementos por página.
     * @return `200 OK` con un `PageResponse` de `StudentDto` (contrato G6).
     * @throws `MethodArgumentTypeMismatchException` si algún parámetro no es numérico; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @GetMapping("/students")
    @Operation(
        summary = "Alumnos de un tutor",
        description = "Página de alumnos vinculados al tutor; `parentId = 0` en la app equivale a `NULL` en la base de datos.",
    )
    @ApiResponse(responseCode = "200", description = "Página de alumnos del tutor.")
    @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun studentsOfParent(
        @Parameter(description = "Id del tutor; 0 representa 'sin tutor' en la app KMP y NULL en la base de datos.", example = "2")
        @RequestParam parentId: Long,
        @Parameter(description = "Índice de página basado en 0.", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Tamaño de página; el controlador lo limita a 100.", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ) = service.studentsOfParent(parentId, PageRequest.of(page, size.coerceAtMost(100), Sort.by("id")))

    /**
     * Vincula un alumno a un grupo a partir del código de clase y actualiza el contador de alumnos del grupo.
     *
     * Capa controller: valida el DTO y delega en [GroupService] la resolución del código y la escritura en una sola transacción; el `parentId` de la app (`0` si no hay tutor) se conserva tal cual en la entidad.
     *
     * @param body datos validados del alumno: tutor, código de clase y nombre.
     * @return `201 Created` con el `StudentDto` vinculado (contrato G7).
     * @throws ApiException con 400 si el nombre está vacío; con 404 si el código de clase no existe; con 409 si la vinculación no se puede completar; se convierte en `ProblemDetail` con ese código.
     * @throws `MethodArgumentNotValidException` si `@Valid` rechaza el cuerpo; se convierte en 400.
     * @throws `HttpMessageNotReadableException` si el JSON no se puede deserializar; se convierte en 400.
     * @throws org.springframework.security.core.AuthenticationException si no se aporta un JWT válido; el filtro de seguridad lo convierte en 401 antes de este método, y una denegación por permisos se convierte en 403.
     * @throws Exception ante un fallo no previsto; se convierte en 500.
     */
    @PostMapping("/students:link")
    @Operation(
        summary = "Vincular alumno a un grupo",
        description = "Busca el grupo por código de clase y registra el alumno, incrementando el contador del grupo para la app KMP.",
    )
    @ApiResponse(responseCode = "201", description = "Alumno vinculado al grupo.")
    @ApiResponse(responseCode = "400", description = "Nombre del alumno vacío.")
    @ApiResponse(responseCode = "404", description = "Código de clase inexistente.")
    @ApiResponse(responseCode = "409", description = "No se pudo completar la vinculación.")
    @ApiResponse(responseCode = "401", description = "Token Bearer ausente o inválido.")
    fun linkStudent(@Valid @RequestBody body: LinkStudentRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.linkStudent(body).toDto())
}
