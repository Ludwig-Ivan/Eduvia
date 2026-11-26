package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.domain.repository.GroupRepository
import com.example.librerias.domain.repository.TaskRepository
import com.example.librerias.domain.util.TodayProvider
import com.example.librerias.model.AppResult
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.NewTaskInput
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.User
import com.example.librerias.model.isBefore
import com.example.librerias.model.parseDateOrNull
import kotlinx.coroutines.flow.Flow
import com.example.librerias.model.Subject

/**
 * Observa las tareas de un grupo. [groupId] es obligatorio: las tareas pertenecen siempre a un
 * grupo y se filtran por ese id (un valor de reserva como 0 no muestra ninguna tarea).
 *
 * @param tasks repositorio de tareas.
 * @return flujo con las tareas del grupo, ordenadas por fecha de entrega.
 */
class ObserveTasksUseCase(private val tasks: TaskRepository) {
    /**
     * @param groupId id real del grupo cuyas tareas se quieren ver.
     * @return flujo de tareas de ese grupo.
     */
    operator fun invoke(groupId: Long): Flow<List<SchoolTask>> = tasks.observeTasks(groupId)
}

/**
 * Marca una entrega de forma idempotente: el alumno se añade a un conjunto de ids, así que
 * repetir la operación no duplica entregas ni cambia el contador. Si la tarea ya no existe,
 * el repositorio devuelve error en lugar de crear una entrega huérfana.
 *
 * @param tasks repositorio de tareas y entregas.
 * @return `AppResult.Success(Unit)`, o `AppResult.Error` si la tarea desapareció.
 */
class MarkTaskDeliveredUseCase(private val tasks: TaskRepository) {
    /**
     * @param taskId tarea entregada.
     * @param studentId alumno que entrega.
     * @return `Unit` en éxito, o el error del repositorio.
     */
    suspend operator fun invoke(taskId: Long, studentId: Long): AppResult<Unit> =
        tasks.markDelivered(taskId, studentId)
}

/**
 * Edita una tarea validando título y fecha. Se permite conservar la fecha original aunque ya
 * haya pasado cuando solo se cambia el texto; en cambio, una fecha nueva nunca puede ser anterior
 * a hoy. Se inyecta [TodayProvider] para que esa comparación sea determinista en los tests.
 *
 * @param tasks repositorio donde se actualiza la tarea.
 * @param today proveedor de la fecha actual.
 * @return la tarea actualizada, o `AppResult.Error` con el error de validación.
 */
class UpdateTaskUseCase(
    private val tasks: TaskRepository,
    private val today: TodayProvider
) {
    /**
     * @param original tarea que se edita; su id se conserva.
     * @param title nuevo título.
     * @param subject nueva materia.
     * @param description nueva descripción.
     * @param dateText nueva fecha de entrega en texto; se acepta la original pasada si no cambia.
     * @return la tarea guardada, o el error correspondiente.
     */
    suspend operator fun invoke(
        original: SchoolTask,
        title: String,
        subject: Subject,
        description: String,
        dateText: String
    ): AppResult<SchoolTask> {
        val now = today.today()
        if (title.isBlank()) return AppResult.Error("Escribe el título de la tarea.")
        val date = parseDateOrNull(dateText, now.year)
            ?: return AppResult.Error("La fecha no es válida. Usa el formato DD/MM/AAAA, por ejemplo 19/11/2026.")
        // Se permite dejar la misma fecha aunque ya haya pasado (solo editas el texto)
        if (date != original.dueDate && date.isBefore(now))
            return AppResult.Error("La fecha de entrega no puede ser anterior a hoy.")

        val updated = original.copy(
            title = title.trim(),
            subject = subject,
            description = description.trim(),
            dueDate = date
        )
        tasks.updateTask(updated)
        return AppResult.Success(updated)
    }
}

/**
 * Elimina la tarea por id; en la API el backend borra también su evento de calendario, que
 * comparte el mismo id que la tarea.
 *
 * @param tasks repositorio de tareas.
 * @return sin valor.
 */
class DeleteTaskUseCase(private val tasks: TaskRepository) {
    /**
     * @param taskId id de la tarea a borrar.
     * @return sin valor.
     */
    suspend operator fun invoke(taskId: Long) = tasks.deleteTask(taskId)
}

/**
 * Crea una tarea validando por pasos (título, fecha no anterior a hoy y existencia del grupo)
 * y, con el id devuelto por el repositorio, crea también su evento de calendario con ESE MISMO id
 * y la fecha de entrega. Así tarea y evento quedan emparejados sin id propio de evento.
 *
 * @param tasks repositorio donde se guarda la tarea.
 * @param groups repositorio usado para comprobar el grupo y tomar su número de alumnos.
 * @param calendar repositorio donde se crea el evento de calendario.
 * @param today proveedor de la fecha actual, usada también como fecha de publicación.
 * @return la tarea guardada con su id real, o `AppResult.Error` si falla alguna validación.
 */
class CreateTaskUseCase(
    private val tasks: TaskRepository,
    private val groups: GroupRepository,
    private val calendar: CalendarRepository,
    private val today: TodayProvider
) {
    /**
     * @param groupId id real del grupo al que pertenece la tarea.
     * @param teacher maestro que publica; de él se toma el nombre.
     * @param input datos del formulario de nueva tarea.
     * @return la tarea creada, o el error de validación o de grupo no encontrado.
     */
    suspend operator fun invoke(
        groupId: Long,
        teacher: User,
        input: NewTaskInput
    ): AppResult<SchoolTask> {
        val now = today.today()

        if (input.title.isBlank()) return AppResult.Error("Escribe el título de la tarea.")
        val dueDate = parseDateOrNull(input.dateText, now.year)
            ?: return AppResult.Error("La fecha no es válida. Ejemplo: 13/10/2026.")
        if (dueDate.isBefore(now)) return AppResult.Error("La fecha de entrega no puede ser anterior a hoy.")

        val group = groups.findById(groupId) ?: return AppResult.Error("No encontramos el grupo.")

        val saved = tasks.addTask(
            SchoolTask(
                id = 0,
                groupId = groupId,
                title = input.title.trim(),
                subject = input.subject,
                description = input.description.trim(),
                dueDate = dueDate,
                publishedDate = now,
                teacherName = teacher.firstName,
                totalStudents = group.studentCount,
                deliveredStudentIds = emptySet()
            )
        )
        calendar.addEvent(
            CalendarEvent(
                id = saved.id, // 👈 EL CAMBIO CLAVE: USA EL ID DE LA TAREA
                groupId = saved.groupId,
                date = dueDate,
                type = CalendarEventType.TASK,
                title = saved.displayTitle,
                subtitle = saved.subject.label
            )
        )
        return AppResult.Success(saved)
    }
}
