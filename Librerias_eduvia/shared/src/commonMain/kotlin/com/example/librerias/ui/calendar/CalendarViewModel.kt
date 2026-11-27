package com.example.librerias.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppError
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Alimenta el calendario: usa `ObserveCalendarUseCase` para listar eventos por rol/grupo y
 * `RespondToMeetingUseCase` para registrar la asistencia. Expone `events` y `actionError` a la UI.
 */
class CalendarViewModel(
    private val container: AppContainer,
    private val role: UserRole,
    private val groupId: Long?,
    private val studentId: Long? = null
) : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError

    fun clearActionError() { _actionError.value = null }

    init {
        viewModelScope.launch {
            container.observeCalendar(role)
                .map { lista ->
                    // 👇 SI ES MAESTRO, FILTRAMOS POR SU GRUPO
                    if (role == UserRole.TEACHER && groupId != null && groupId != 0L) {
                        lista.filter { it.groupId == groupId }
                    } else {
                        lista
                    }
                }
                .collect { lista ->
                    _events.value = lista
                }
        }
    }

    fun updateAttendance(eventId: Long, attending: Boolean) {
        viewModelScope.launch {
            _actionError.value = null
            val response = if (attending) MeetingResponse.ATTENDING else MeetingResponse.DECLINED

            try {
                // Solo cambia la vista local si la API aceptó el cambio: evita confirmar una
                // asistencia que no se guardó cuando el servidor está apagado.
                container.respondToMeeting(
                    meetingId = eventId,
                    studentId = studentId ?: 1L,
                    response = response,
                )
                _events.update { lista ->
                    lista.map { event ->
                        if (event.id == eventId) {
                            val isChanging = event.subtitle.contains("Confirmaste", true) ||
                                event.subtitle.contains("Avisaste", true)
                            val newSubtitle = when {
                                isChanging -> "Pendiente"
                                attending -> "Confirmaste asistencia"
                                else -> "Avisaste que no puedes"
                            }
                            event.copy(subtitle = newSubtitle)
                        } else {
                            event
                        }
                    }
                }
            } catch (e: Exception) {
                _actionError.value = container.errorOf(e)
            }
        }
    }
}
