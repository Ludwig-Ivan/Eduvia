package com.example.librerias.data.repository

import com.example.librerias.data.CalendarSampleData
import com.example.librerias.data.TeacherCalendarSampleData
import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria del calendario anterior a la API, conservada como reserva y ejemplo
 * (HALLAZGOS H10, OPEN-QUESTIONS Q12); la ruta activa es `data/remote`, inyectada por `AppContainer`.
 */
class CalendarRepositoryImpl : CalendarRepository {

    private val parentEvents = MutableStateFlow(CalendarSampleData.events)
    private val teacherEvents = MutableStateFlow(TeacherCalendarSampleData.events)
    private var nextId = maxOf(
        CalendarSampleData.events.maxOfOrNull { it.id } ?: 0L,
        TeacherCalendarSampleData.events.maxOfOrNull { it.id } ?: 0L
    ) + 1

    override fun observeEvents(role: UserRole): Flow<List<CalendarEvent>> = when (role) {
        UserRole.PARENT -> parentEvents
        UserRole.TEACHER -> teacherEvents
    }

    override suspend fun addEvent(event: CalendarEvent) {
        val id = if (event.id > 0) event.id else nextId++   // ✅ CAMBIO AQUÍ
        teacherEvents.update { it + event.copy(id = id) }
        parentEvents.update { it + event.copy(id = id, attendance = null) }
    }

    override suspend fun deleteEvent(eventId: Long) {
        teacherEvents.update { list -> list.filter { it.id != eventId } }
        parentEvents.update { list -> list.filter { it.id != eventId } }
    }


}