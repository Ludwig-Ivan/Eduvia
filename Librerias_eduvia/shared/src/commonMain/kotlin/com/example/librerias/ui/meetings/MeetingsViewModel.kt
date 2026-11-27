package com.example.librerias.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppError
import com.example.librerias.model.AppResult
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.NewMeetingInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gestiona las juntas del grupo: observa juntas, respuestas y asistencia, y usa crear,
 * actualizar, responder y borrar juntas. Expone `meetings`, `creationError` (formulario) y
 * `actionError` (respuesta/borrado) a la UI.
 */
class MeetingsViewModel(
    private val container: AppContainer,
    private val groupId: Long?,
    private val studentId: Long? = null      // null = maestro
) : ViewModel() {

    val meetings: StateFlow<List<Meeting>> = combine(
        container.observeMeetings(),
        if (studentId == null) flowOf(emptyMap()) else container.observeStudentResponses(studentId)
    ) { list, responses ->
        list
            .filter { it.groupId == groupId || groupId == null }
            .map { m ->
                if (studentId == null) m
                else m.copy(response = responses[m.id] ?: MeetingResponse.PENDING)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _creationError = MutableStateFlow<String?>(null)
    val creationError: StateFlow<String?> = _creationError

    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError

    private var isCreatingMeeting = false

    fun clearCreationError() { _creationError.value = null }
    fun clearActionError() { _actionError.value = null }

    fun respondToMeeting(meetingId: Long, studentId: Long, response: MeetingResponse) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                container.respondToMeeting(meetingId, studentId, response)
            } catch (e: Exception) {
                _actionError.value = container.errorOf(e)
            }
        }
    }

    fun getAttendanceFlow(meetingId: Long) = container.observeAttendance(meetingId)

    fun createMeeting(input: NewMeetingInput, onSuccess: () -> Unit) {
        if (isCreatingMeeting) return
        isCreatingMeeting = true

        viewModelScope.launch {
            try {
                _creationError.value = null
                // Ya no inventamos valores: si falta algo, el caso de uso explica qué.
                val validInput = input.copy(groupId = groupId ?: 0L)

                when (val result = container.createMeeting(validInput)) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Error -> _creationError.value = result.message
                }
            } catch (e: Exception) {
                _creationError.value = container.errorMessage(e)
            } finally {
                isCreatingMeeting = false
            }
        }
    }

    fun updateMeeting(original: Meeting, input: NewMeetingInput, onSuccess: () -> Unit) {
        if (isCreatingMeeting) return
        isCreatingMeeting = true

        viewModelScope.launch {
            try {
                _creationError.value = null
                val validInput = input.copy(groupId = groupId ?: original.groupId)
                when (val result = container.updateMeeting(
                    original,
                    validInput.title,
                    validInput.dateText,
                    validInput.time,
                    validInput.place,
                    validInput.link,
                    validInput.reminder,
                )) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Error -> _creationError.value = result.message
                }
            } catch (e: Exception) {
                _creationError.value = container.errorMessage(e)
            } finally {
                isCreatingMeeting = false
            }
        }
    }

    fun deleteMeeting(meetingId: Long) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                container.deleteMeeting(meetingId)
            } catch (e: Exception) {
                _actionError.value = container.errorOf(e)
            }
        }
    }
}
