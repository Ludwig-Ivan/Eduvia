package com.example.librerias.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppResult
import com.example.librerias.model.NewTaskInput
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.Subject
import com.example.librerias.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gestiona las tareas del grupo con `ObserveTasksUseCase`, `CreateTaskUseCase`,
 * `UpdateTaskUseCase`, `MarkTaskDeliveredUseCase` y `DeleteTaskUseCase`.
 * Expone `tasks` y `creationError` a la UI.
 */
class TasksViewModel(
    private val container: AppContainer,
    private val groupId: Long,
    private val teacher: User
) : ViewModel() {

    val tasks: StateFlow<List<SchoolTask>> = container.observeTasks(groupId = groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _creationError = MutableStateFlow<String?>(null)
    val creationError: StateFlow<String?> = _creationError

    fun clearCreationError() { _creationError.value = null }

    fun markAsDelivered(taskId: Long, studentId: Long) {
        viewModelScope.launch {
            container.markTaskDelivered(taskId, studentId)
        }
    }

    fun updateTask(
        original: SchoolTask,
        title: String,
        subject: Subject,
        description: String,
        dateText: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _creationError.value = null
            when (val r = container.updateTask(original, title, subject, description, dateText)) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> _creationError.value = r.message
            }
        }
    }

    fun createTask(input: NewTaskInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _creationError.value = null

            when (val result = container.createTask(groupId = groupId, teacher = teacher, input = input)) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> _creationError.value = result.message
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            container.deleteTask(taskId)
        }
    }
}