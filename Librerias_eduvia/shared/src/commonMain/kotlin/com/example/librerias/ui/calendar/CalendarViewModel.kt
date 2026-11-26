package com.example.librerias.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
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
 * `RespondToMeetingUseCase` para registrar la asistencia. Expone `events` a la UI.
 */
class CalendarViewModel(
    private val container: AppContainer,
    private val role: UserRole,
    private val groupId: Long?,
    private val studentId: Long? = null
) : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

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
            val response = if (attending) MeetingResponse.ATTENDING else MeetingResponse.DECLINED

            // 1. Guardamos en el repositorio real de juntas (para el maestro)
            container.respondToMeeting(
                meetingId = eventId,
                studentId = studentId ?: 1L,
                response = response
            )

            // 2. Actualizamos la vista del calendario al instante (para el padre)
            _events.update { lista ->
                lista.map { event ->
                    if (event.id == eventId) {
                        val isChanging = event.subtitle.contains("Confirmaste", true) || event.subtitle.contains("Avisaste", true)
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
        }
    }
}