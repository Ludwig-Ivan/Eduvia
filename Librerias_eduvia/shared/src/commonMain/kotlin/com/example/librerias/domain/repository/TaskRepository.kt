package com.example.librerias.domain.repository

import com.example.librerias.model.AppResult
import com.example.librerias.model.SchoolTask
import kotlinx.coroutines.flow.Flow

/**
 * Capa de dominio: tareas escolares y entregas de los alumnos.
 *
 * Lo satisfacen `TaskRepositoryImpl` (en memoria) y `RemoteTaskRepository` (HTTP: `tasks`,
 * `tasks/{id}` y `tasks/{id}/deliveries`; el backend crea y borra el evento de calendario
 * asociado). Los métodos corresponden al contrato de API y devuelven modelos del dominio.
 */
interface TaskRepository {
    /**
     * Observa las tareas de un grupo. [groupId] es obligatorio y debe ser el id real del grupo:
     * filtra por ese valor; un id de reserva como 0 no devuelve tareas.
     * @return flujo con las tareas del grupo, ordenadas por fecha de entrega.
     */
    fun observeTasks(groupId: Long): Flow<List<SchoolTask>>

    /**
     * Guarda una tarea nueva; el repositorio le asigna el id.
     * @return la tarea persistida con su id real.
     */
    suspend fun addTask(task: SchoolTask): SchoolTask           // asigna el id

    /**
     * Actualiza la tarea conservando su id.
     * @return sin valor; los observadores reciben la tarea modificada.
     */
    suspend fun updateTask(task: SchoolTask)

    /**
     * Registra la entrega de [studentId] en [taskId]. Volver a marcarla no duplica la entrega:
     * el alumno se añade a un conjunto de ids.
     * @return `AppResult.Success`, o `AppResult.Error` si la tarea ya no existe.
     */
    suspend fun markDelivered(taskId: Long, studentId: Long): AppResult<Unit>

    /**
     * Elimina la tarea [taskId] (y en remoto su evento de calendario).
     * @return sin valor.
     */
    suspend fun deleteTask(taskId: Long)
}
