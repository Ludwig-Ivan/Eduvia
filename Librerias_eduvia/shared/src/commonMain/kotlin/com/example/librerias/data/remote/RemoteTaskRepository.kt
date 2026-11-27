package com.example.librerias.data.remote

import com.example.librerias.domain.repository.TaskRepository
import com.example.librerias.model.AppFailureException
import com.example.librerias.model.AppResult
import com.example.librerias.model.SchoolTask
import kotlinx.coroutines.flow.Flow

/**
 * Tareas leídas y escritas en la API; el backend crea/borra el evento de calendario asociado.
 *
 * Implementa [TaskRepository] sobre HTTP (T1-T6) y mapea `TaskDto` a [SchoolTask] con `toModel`.
 * Los listados se cachean por `groupId` en [RemoteListStore] para que la UI observe cambios sin
 * esperar la red; el evento de calendario de una tarea usa el mismo id, por lo que al borrarla se
 * retira ese id de la caché de calendario. `markDelivered` devuelve `AppResult.Error`; el resto de
 * operaciones propaga la `ApiException` del backend.
 */
class RemoteTaskRepository(
    private val api: ApiClient,
    private val calendar: RemoteCalendarRepository? = null,
    reporter: RemoteErrorReporter? = null,
) : TaskRepository {

    private val byGroup = RemoteListStore<Long, SchoolTask>(reporter)

    /**
     * `GET tasks?groupId=` (T1): lee `PageDto<TaskDto>` y mapea cada tarea a [SchoolTask], cacheando
     * el listado por `groupId`. Si la carga falla, la caché conserva el último valor conocido.
     */
    override fun observeTasks(groupId: Long): Flow<List<SchoolTask>> =
        byGroup.flow(groupId) {
            api.get<PageDto<TaskDto>>("tasks?groupId=$groupId&page=0&size=100")
                .items.map { it.toModel() }
        }

    /**
     * `POST tasks` (T3): envía [CreateTaskRequestDto], mapea la respuesta a [SchoolTask] y la añade
     * a la caché del grupo. El backend crea en la misma transacción el evento de calendario con el
     * mismo id, así que no se hace otra llamada. Propaga errores de la API.
     */
    override suspend fun addTask(task: SchoolTask): SchoolTask = try {
        val dto: TaskDto = api.post("tasks", task.toCreateRequest())
        val saved = dto.toModel()
        byGroup.update(saved.groupId) { current ->
            if (current.any { it.id == saved.id }) current else current + saved
        }
        saved
    } catch (e: Exception) {
        throw AppFailureException(api.errorOf(e))
    }

    /**
     * `PUT tasks/{id}` (T4): envía [UpdateTaskRequestDto], mapea la tarea actualizada y sustituye la
     * entrada correspondiente en la caché del grupo. El backend actualiza el evento de calendario,
     * por lo que se refresca la caché del calendario si está disponible. Propaga errores de la API.
     */
    override suspend fun updateTask(task: SchoolTask) = try {
        val dto: TaskDto = api.put("tasks/${task.id}", task.toUpdateRequest())
        val saved = dto.toModel()
        byGroup.update(saved.groupId) { current -> current.map { if (it.id == saved.id) saved else it } }
        // El backend también actualiza el evento de calendario.
        calendar?.refreshEvents()
        Unit
    } catch (e: Exception) {
        throw AppFailureException(api.errorOf(e))
    }

    /**
     * `POST tasks/{id}/deliveries` (T6): envía [MarkDeliveredRequestDto], mapea la tarea devuelta y
     * reemplaza su entrada en la caché del grupo con la nueva lista de entregas. Errores →
     * `AppResult.Error` con el mensaje de la API.
     */
    override suspend fun markDelivered(taskId: Long, studentId: Long): AppResult<Unit> = try {
        val dto: TaskDto = api.post("tasks/$taskId/deliveries", MarkDeliveredRequestDto(studentId))
        val updated = dto.toModel()
        byGroup.update(updated.groupId) { current -> current.map { if (it.id == updated.id) updated else it } }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        api.failure(e)
    }

    /**
     * `GET tasks/{id}` (T2) para conocer el grupo y `DELETE tasks/{id}` (T5, sin cuerpo). Quita la
     * tarea de la caché de ese grupo (o de todas si no se pudo leer el detalle) y elimina el evento
     * de calendario con el mismo id. El GET fallido se ignora; el DELETE propaga errores de la API.
     */
    override suspend fun deleteTask(taskId: Long) = try {
        val existing = runCatching { api.get<TaskDto>("tasks/$taskId").toModel() }.getOrNull()
        api.deleteNoContent("tasks/$taskId")
        if (existing != null) {
            byGroup.update(existing.groupId) { current -> current.filterNot { it.id == taskId } }
        } else {
            byGroup.removeWhereAll { it.id == taskId }
        }
        calendar?.removeLocalEvent(taskId)
        Unit
    } catch (e: Exception) {
        throw AppFailureException(api.errorOf(e))
    }

    /** Vuelve a descargar los listados observados; lo usa el botón de reintento de la UI. */
    suspend fun refreshAll() = byGroup.refreshAll()
}
