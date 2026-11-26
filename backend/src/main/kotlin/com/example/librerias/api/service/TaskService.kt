package com.example.librerias.api.service

import com.example.librerias.api.domain.CalendarEvent
import com.example.librerias.api.domain.CalendarEventType
import com.example.librerias.api.domain.SchoolTask
import com.example.librerias.api.domain.TaskDelivery
import com.example.librerias.api.repository.CalendarEventRepository
import com.example.librerias.api.repository.SchoolGroupRepository
import com.example.librerias.api.repository.SchoolTaskRepository
import com.example.librerias.api.repository.StudentRepository
import com.example.librerias.api.repository.TaskDeliveryRepository
import com.example.librerias.api.repository.UserRepository
import com.example.librerias.api.web.badRequest
import com.example.librerias.api.web.conflict
import com.example.librerias.api.web.notFound
import com.example.librerias.api.web.dto.FieldErrorDto
import com.example.librerias.api.web.dto.PageResponse
import com.example.librerias.api.web.dto.toDto
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Servicio de tareas escolares y de sus entregas.
 *
 * Responsabilidad: crear, editar y borrar tareas junto con su evento de calendario (que comparte
 * `calendar_events.id = school_tasks.id`, regla de la app para no duplicar identificadores), marcar
 * entregas de forma idempotente y calcular el estado que muestra la app (`DELIVERED`, `EXPIRED` o
 * `PENDING`).
 *
 * Colaboradores: SchoolTaskRepository, CalendarEventRepository, SchoolGroupRepository,
 * StudentRepository, TaskDeliveryRepository y UserRepository. Las escrituras que afectan a tarea,
 * evento y entregas comparten transacción para no dejar la agenda desincronizada.
 */
@Service
class TaskService(
    private val tasks: SchoolTaskRepository,
    private val groups: SchoolGroupRepository,
    private val students: StudentRepository,
    private val deliveries: TaskDeliveryRepository,
    private val events: CalendarEventRepository,
    private val users: UserRepository,
) {
    /**
     * Lista las tareas de un grupo con el número de alumnos que ya entregó; el controlador las
     * ordena por fecha de vencimiento, que es como la app las presenta.
     *
     * @param groupId grupo cuyas tareas se consultan.
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `SchoolTaskDto`.
     * @throws ApiException 404 si el grupo no existe; así se evita confundir «sin tareas» con un
     * grupo inexistente.
     */
    fun list(groupId: Long, pageable: Pageable): PageResponse<com.example.librerias.api.web.dto.SchoolTaskDto> {
        if (!groups.existsById(groupId)) throw notFound("Grupo no encontrado")
        val page = tasks.findAllByGroupId(groupId, pageable)
        return PageResponse(
            items = page.content.map { toDtoWithDeliveries(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    /**
     * Devuelve el detalle de una tarea con su estado calculado a partir de las entregas y de la
     * fecha de vencimiento.
     *
     * @param id identificador de la tarea.
     * @return `SchoolTaskDto` con `deliveredStudentIds` y `status`.
     * @throws ApiException 404 si la tarea no existe.
     */
    fun get(id: Long): com.example.librerias.api.web.dto.SchoolTaskDto {
        val task = tasks.findById(id).orElseThrow { notFound("Tarea no encontrada") }
        return toDtoWithDeliveries(task, statusOf(task))
    }

    /**
     * Crea la tarea y su evento de calendario en la misma transacción, usando exactamente el mismo
     * id para ambos: la app trata la tarea y su recordatorio como una sola entidad. Se guarda una
     * instantánea del nombre del docente y del número de alumnos del grupo porque es lo que muestra
     * la tarjeta de la tarea, aunque el grupo cambie después.
     *
     * @param groupId grupo al que pertenece la tarea.
     * @param title título obligatorio.
     * @param subject asignatura de la tarea.
     * @param description enunciado de la tarea.
     * @param dateText fecha de vencimiento en `YYYY-MM-DD`.
     * @return `SchoolTaskDto` de la tarea creada.
     * @throws ApiException 400 si el título está vacío o la fecha no usa el formato ISO del
     * contrato; el error por campo permite corregir el formulario en la app.
     * @throws ApiException 404 si el grupo o su docente no existen; sin docente no se puede
     * completar la tarjeta que consume la app.
     */
    @Transactional
    fun create(
        groupId: Long,
        title: String,
        subject: com.example.librerias.api.domain.Subject,
        description: String,
        dateText: String,
    ): com.example.librerias.api.web.dto.SchoolTaskDto {
        if (title.isBlank()) throw badRequest("Título obligatorio", listOf(FieldErrorDto("title", "Requerido")))
        val date = parseDate(dateText)
        val group = groups.findById(groupId).orElseThrow { notFound("Grupo no encontrado") }
        val teacher = users.findById(group.teacherId).orElseThrow { notFound("Docente no encontrado") }
        val task = tasks.save(
            SchoolTask(
                groupId = groupId,
                title = title.trim(),
                subject = subject,
                description = description.trim(),
                dueDate = date,
                publishedDate = LocalDate.now(),
                teacherName = teacher.fullName,
                totalStudents = group.studentCount,
            ),
        )
        // Misma transacción: el evento comparte el id de la tarea.
        events.save(
            CalendarEvent(
                id = task.id,
                groupId = groupId,
                date = date,
                type = CalendarEventType.TASK,
                title = task.title,
                subtitle = subject.name,
            ),
        )
        return toDtoWithDeliveries(task)
    }

    /**
     * Actualiza la tarea y propaga título, asignatura y fecha al evento con el mismo id, de modo
     * que la agenda no siga mostrando el vencimiento anterior. La identidad de la tarea no cambia.
     *
     * @param id identificador de la tarea.
     * @param title título obligatorio.
     * @param subject asignatura nueva.
     * @param description enunciado nuevo.
     * @param dateText nueva fecha en `YYYY-MM-DD`.
     * @return `SchoolTaskDto` con el estado recalculado.
     * @throws ApiException 400 si el título está vacío o la fecha es inválida.
     * @throws ApiException 404 si la tarea no existe.
     */
    @Transactional
    fun update(
        id: Long,
        title: String,
        subject: com.example.librerias.api.domain.Subject,
        description: String,
        dateText: String,
    ): com.example.librerias.api.web.dto.SchoolTaskDto {
        val task = tasks.findById(id).orElseThrow { notFound("Tarea no encontrada") }
        if (title.isBlank()) throw badRequest("Título obligatorio")
        val date = parseDate(dateText)
        task.title = title.trim()
        task.subject = subject
        task.description = description.trim()
        task.dueDate = date
        task.updatedAt = java.time.Instant.now()
        tasks.save(task)
        events.findById(id).ifPresent { event ->
            event.date = date
            event.title = task.title
            event.subtitle = subject.name
            events.save(event)
        }
        return toDtoWithDeliveries(task, statusOf(task))
    }

    /**
     * Elimina las entregas y el evento asociados antes que la tarea, en una sola transacción: una
     * entrega huérfana seguiría contando como alumno que entregó y un evento huérfano seguiría
     * apareciendo en la agenda de la app.
     *
     * @param id identificador de la tarea.
     * @return Unit; el controlador responde 204.
     * @throws ApiException 404 si la tarea no existe.
     */
    @Transactional
    fun delete(id: Long) {
        val task = tasks.findById(id).orElseThrow { notFound("Tarea no encontrada") }
        deliveries.deleteByTaskId(id)
        events.findById(id).ifPresent { events.delete(it) }
        tasks.delete(task)
    }

    /**
     * Marca la entrega de un alumno. Repetir la llamada no crea una segunda entrega: la clave
     * primaria compuesta por tarea y alumno vuelve idempotente la operación, necesario porque la
     * app puede reintentar peticiones tras un 401 con refresco de token.
     *
     * @param taskId tarea entregada.
     * @param studentId alumno que entrega.
     * @return `SchoolTaskDto` con la lista de entregas y el estado actualizados.
     * @throws ApiException 404 si la tarea o el alumno no existen.
     * @throws ApiException 409 si el alumno pertenece a otro grupo; solo los alumnos del grupo de
     * la tarea pueden entregarla, tal como exige el contrato de la app.
     */
    @Transactional
    fun markDelivered(taskId: Long, studentId: Long): com.example.librerias.api.web.dto.SchoolTaskDto {
        val task = tasks.findById(taskId).orElseThrow { notFound("Tarea no encontrada") }
        val student = students.findById(studentId).orElseThrow { notFound("Alumno no encontrado") }
        if (student.groupId != task.groupId) {
            throw conflict("El alumno no pertenece al grupo de la tarea")
        }
        if (!deliveries.existsByTaskIdAndStudentId(taskId, studentId)) {
            deliveries.save(TaskDelivery(task = task, student = student))
        }
        return toDtoWithDeliveries(task, statusOf(task))
    }

    private fun toDtoWithDeliveries(
        task: SchoolTask,
        status: String? = null,
    ) = task.toDto(tasks.findDeliveredStudentIds(task.id), status)

    private fun statusOf(task: SchoolTask): String {
        val delivered = tasks.findDeliveredStudentIds(task.id).size
        return when {
            delivered >= task.totalStudents && task.totalStudents > 0 -> "DELIVERED"
            task.dueDate.isBefore(LocalDate.now()) -> "EXPIRED"
            else -> "PENDING"
        }
    }

    private fun parseDate(text: String): LocalDate = try {
        LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        throw badRequest("Fecha inválida, use YYYY-MM-DD", listOf(FieldErrorDto("dateText", "Formato YYYY-MM-DD")))
    }
}
