package com.example.librerias.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppError
import com.example.librerias.model.AppErrorKind
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
 * Expone `tasks`, `creationError` (formulario) y `actionError` (borrado/entrega) a la UI.
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

    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError

    fun clearCreationError() { _creationError.value = null }
    fun clearActionError() { _actionError.value = null }

    /** Los errores de validación no traen `AppError`; se envuelven para la banner genérica. */
    private fun AppResult.Error.asAppError(): AppError =
        error ?: AppError(AppErrorKind.VALIDATION, message)

    fun markAsDelivered(taskId: Long, studentId: Long) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                when (val r = container.markTaskDelivered(taskId, studentId)) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> _actionError.value = r.asAppError()
                }
            } catch (e: Exception) {
                _actionError.value = container.errorOf(e)
            }
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
            try {
                when (val r = container.updateTask(original, title, subject, description, dateText)) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Error -> _creationError.value = r.message
                }
            } catch (e: Exception) {
                _creationError.value = container.errorMessage(e)
            }
        }
    }

    fun createTask(input: NewTaskInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _creationError.value = null
            try {
                when (val result = container.createTask(groupId = groupId, teacher = teacher, input = input)) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Error -> _creationError.value = result.message
                }
            } catch (e: Exception) {
                _creationError.value = container.errorMessage(e)
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                container.deleteTask(taskId)
            } catch (e: Exception) {
                _actionError.value = container.errorOf(e)
            }
        }
    }
}
