package com.example.librerias.data.repository

import com.example.librerias.data.SchoolSampleData
import com.example.librerias.domain.repository.TaskRepository
import com.example.librerias.model.AppResult
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.toEpochDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de las tareas escolares anterior a la API, conservada como reserva y ejemplo
 * (HALLAZGOS H10, OPEN-QUESTIONS Q12); la ruta activa es `data/remote`, inyectada por `AppContainer`.
 */
class TaskRepositoryImpl : TaskRepository {

    private val tasks = MutableStateFlow(SchoolSampleData.tasks)
    private var nextId = (SchoolSampleData.tasks.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeTasks(groupId: Long): Flow<List<SchoolTask>> =
        tasks.map { list ->
            list.filter { it.groupId == groupId }.sortedBy { it.dueDate.toEpochDay() }
        }

    override suspend fun addTask(task: SchoolTask): SchoolTask {
        val saved = task.copy(id = nextId++)
        tasks.update { it + saved }
        return saved
    }

    override suspend fun updateTask(task: SchoolTask) {
        tasks.update { list -> list.map { if (it.id == task.id) task else it } }
    }

    override suspend fun markDelivered(taskId: Long, studentId: Long): AppResult<Unit> {
        val task = tasks.value.firstOrNull { it.id == taskId }
            ?: return AppResult.Error("La tarea ya no existe.")

        // 👇 ESTE ES EL CAMBIO: Actualizamos la lista directamente para que el Flow emita el cambio
        val updatedTask = task.copy(deliveredStudentIds = task.deliveredStudentIds + studentId)
        tasks.update { list ->
            list.map { if (it.id == taskId) updatedTask else it }
        }

        return AppResult.Success(Unit)
    }

    override suspend fun deleteTask(taskId: Long) {
        tasks.update { list -> list.filter { it.id != taskId } }
    }
}