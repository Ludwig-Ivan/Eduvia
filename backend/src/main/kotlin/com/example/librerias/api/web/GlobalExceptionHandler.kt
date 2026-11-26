package com.example.librerias.api.web

import com.example.librerias.api.web.dto.FieldErrorDto
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Error de dominio con estado HTTP, mensaje y errores por campo controlados por el backend.
 *
 * Los servicios lo lanzan en lugar de decidir cómo se serializa la respuesta; [GlobalExceptionHandler] es quien lo convierte en `ProblemDetail` (RFC 9457), de modo que la capa controller siga siendo solo una traducción de HTTP a llamadas de servicio.
 */
class ApiException(
    val status: HttpStatus,
    message: String,
    val errors: List<FieldErrorDto> = emptyList(),
) : RuntimeException(message)

/**
 * Construye un error de validación o regla de negocio; [GlobalExceptionHandler] lo convierte en 400 con `ProblemDetail`.
 *
 * @param message detalle que se mostrará al usuario de la app KMP.
 * @param errors errores por campo que se añaden a la propiedad `errors` del `ProblemDetail`.
 * @return [ApiException] con `HttpStatus.BAD_REQUEST`.
 * @throws Nothing no lanza nada: solo construye el error para que lo maneje el advice.
 */
fun badRequest(message: String, errors: List<FieldErrorDto> = emptyList()) =
    ApiException(HttpStatus.BAD_REQUEST, message, errors)

/**
 * Construye un error por recurso inexistente; [GlobalExceptionHandler] lo convierte en 404 con `ProblemDetail`.
 *
 * @param message detalle del recurso que no se encontró.
 * @return [ApiException] con `HttpStatus.NOT_FOUND`.
 * @throws Nothing no lanza nada: solo construye el error para que lo maneje el advice.
 */
fun notFound(message: String) = ApiException(HttpStatus.NOT_FOUND, message)

/**
 * Construye un error por conflicto de reglas de negocio (duplicidades o pertenencia incorrecta); [GlobalExceptionHandler] lo convierte en 409 con `ProblemDetail`.
 *
 * @param message detalle del conflicto detectado por el servicio.
 * @return [ApiException] con `HttpStatus.CONFLICT`.
 * @throws Nothing no lanza nada: solo construye el error para que lo maneje el advice.
 */
fun conflict(message: String) = ApiException(HttpStatus.CONFLICT, message)

/**
 * Construye un error de credenciales inválidas del dominio; [GlobalExceptionHandler] lo convierte en 401 con `ProblemDetail`.
 *
 * El 401 de ausencia de token lo resuelve directamente la capa de seguridad; esta factoría cubre los casos detectados por [com.example.librerias.api.service.AuthService].
 *
 * @param message detalle de la credencial rechazada.
 * @return [ApiException] con `HttpStatus.UNAUTHORIZED`.
 * @throws Nothing no lanza nada: solo construye el error para que lo maneje el advice.
 */
fun unauthorized(message: String) = ApiException(HttpStatus.UNAUTHORIZED, message)

/**
 * Capa error: centraliza la conversión de excepciones a `ProblemDetail` (RFC 9457) para toda la API REST.
 *
 * `@Order(Ordered.HIGHEST_PRECEDENCE)` es necesario porque Spring Boot registra sus propios resolutores para fallos de binding y validación. Sin esta precedencia, los `@Valid` fallidos, los cuerpos ilegibles y los tipos inválidos de path/query se convertirían en la respuesta genérica de Boot en vez de llegar a estos `@ExceptionHandler`, perdiéndose el formato `type/title/status/detail` y la propiedad `errors` que espera el contrato con la app KMP.
 *
 * Cada `@ExceptionHandler` cubre una causa concreta: [ApiException] para errores de dominio con estado explícito (400/401/404/409), [MethodArgumentNotValidException] para `@Valid`, [HttpMessageNotReadableException] para JSON no deserializable, [MethodArgumentTypeMismatchException] para valores de query o path no convertibles (400) y [Exception] para fallos no previstos (500, registrados en logs sin exponer detalles internos). El 403 no se trata aquí: lo produce la capa de seguridad cuando el token carece de permisos.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {

    /**
     * Convierte un error de dominio en `ProblemDetail`, conservando el estado que el servicio decidió.
     *
     * Capa error: toma el mensaje y la lista de errores por campo del [ApiException] y rellena `title` con la razón del estado; es el camino por el que los servicios de autenticación, grupos, tareas, juntas y calendario comunican 400, 401, 404 y 409 a la app KMP.
     *
     * @param ex error lanzado por un servicio con su `status`, mensaje y errores de campo.
     * @return `ResponseEntity<ProblemDetail>` con el mismo código HTTP del error de dominio (400/401/404/409).
     * @throws Exception no se propaga: la excepción se transforma por completo en la respuesta indicada; los fallos no previstos los atiende [handleGeneric] como 500.
     */
    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(ex.status, ex.message ?: ex.status.reasonPhrase)
        pd.title = ex.status.reasonPhrase
        pd.setProperty("errors", ex.errors)
        return ResponseEntity.status(ex.status).body(pd)
    }

    /**
     * Convierte los fallos de `@Valid` en una respuesta 400 con `ProblemDetail` y errores por campo.
     *
     * Capa error: recorre todos los errores de enlazado; los de campo conservan su nombre y los errores de objeto usan el nombre del objeto para que la app KMP pueda resaltar el control correspondiente. Gracias a `@Order(HIGHEST_PRECEDENCE)`, este handler gana al manejador genérico de Boot.
     *
     * @param ex excepción producida al validar un `@RequestBody` anotado con `@Valid`.
     * @return `ResponseEntity<ProblemDetail>` con 400, título `Bad Request` y la propiedad `errors`.
     * @throws Exception no se propaga: la validación se transforma por completo en la respuesta 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.allErrors.map { error ->
            if (error is FieldError) {
                FieldErrorDto(error.field, error.defaultMessage ?: "inválido")
            } else {
                FieldErrorDto(error.objectName, error.defaultMessage ?: "inválido")
            }
        }
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validación incorrecta")
        pd.title = "Bad Request"
        pd.setProperty("errors", errors)
        return ResponseEntity.badRequest().body(pd)
    }

    /**
     * Convierte un cuerpo JSON ilegible o no deserializable en una respuesta 400 con `ProblemDetail`.
     *
     * Capa error: registra la causa más específica para diagnóstico, pero expone al cliente solo un error genérico asociado al campo `body`, evitando filtrar detalles de la deserialización. Forma parte de los manejadores que `@Order(HIGHEST_PRECEDENCE)` prioritiza sobre el error por defecto de Boot.
     *
     * @param ex excepción lanzada por el convertidor de mensajes al leer el cuerpo de la petición.
     * @return `ResponseEntity<ProblemDetail>` con 400 y un error de campo `body`.
     * @throws Exception no se propaga: el fallo de lectura se transforma por completo en la respuesta 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
            .warn("Cuerpo de petición no legible: {}", ex.mostSpecificCause.message)
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Cuerpo de petición inválido")
        pd.title = "Bad Request"
        pd.setProperty("errors", listOf(FieldErrorDto("body", "No se pudo deserializar")))
        return ResponseEntity.badRequest().body(pd)
    }

    /**
     * Convierte valores no compatibles en parámetros de query o path en una respuesta 400 con `ProblemDetail`.
     *
     * Capa error: cubre, por ejemplo, un `id` no numérico o un enum de rol con un literal distinto de los definidos en el contrato KMP; el nombre del parámetro y el valor rechazado se devuelven en `errors` para orientar al cliente. Sin la precedencia máxima del advice, Boot respondería este caso con su formato genérico.
     *
     * @param ex excepción de conversión de tipo de un argumento de la petición.
     * @return `ResponseEntity<ProblemDetail>` con 400 y el detalle del parámetro inválido.
     * @throws Exception no se propaga: el fallo de conversión se transforma por completo en la respuesta 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Valor inválido para ${ex.name}")
        pd.title = "Bad Request"
        pd.setProperty(
            "errors",
            listOf(FieldErrorDto(ex.name ?: "request", "Valor no permitido: ${ex.value}")),
        )
        return ResponseEntity.badRequest().body(pd)
    }

    /**
     * Última red de seguridad: convierte cualquier excepción no prevista en una respuesta 500 con `ProblemDetail`.
     *
     * Capa error: registra la traza completa en logs para diagnóstico y devuelve al cliente un mensaje genérico, sin exponer información interna. Se invoca solo cuando ningún handler anterior, incluidos los de validación y tipos inválidos, ha gestionado la excepción.
     *
     * @param ex excepción no controlada por los handlers específicos.
     * @return `ResponseEntity<ProblemDetail>` con 500, título `Internal Server Error` y detalle genérico.
     * @throws Exception no se propaga: se registra y se transforma por completo en la respuesta 500.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ProblemDetail> {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
            .error("Error no controlado al procesar la petición", ex)
        val pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error interno del servidor",
        )
        pd.title = "Internal Server Error"
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd)
    }
}
