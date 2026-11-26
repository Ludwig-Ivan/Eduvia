package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Observa el calendario según el rol: el maestro ve el resumen de asistencia de sus eventos
 * y el padre su propia vista. El repositorio remoto mantiene la caché por rol, así que basta
 * con pasar el rol: TASK y MEETING solo refrescan la caché local (el backend ya creó su evento
 * con la tarea o la junta) y solo EXAM hace un POST a `calendar/events`.
 *
 * @param calendar repositorio de calendario.
 * @return flujo con los eventos visibles para el rol indicado.
 */
class ObserveCalendarUseCase(private val calendar: CalendarRepository) {
    /**
     * @param role rol del usuario que consulta el calendario.
     * @return flujo de eventos correspondiente a ese rol.
     */
    operator fun invoke(role: UserRole): Flow<List<CalendarEvent>> = calendar.observeEvents(role)

}

/**
 * Borra un evento por id; en la API es DELETE `calendar/events/{id}` y también se retira de
 * la caché local para que la UI deje de mostrarlo de inmediato.
 *
 * @param calendar repositorio de calendario.
 * @return sin valor.
 */
class DeleteEventUseCase(private val calendar: CalendarRepository) {
    /**
     * @param eventId id del evento a eliminar.
     * @return sin valor; los observadores reciben la lista sin ese evento.
     */
    suspend operator fun invoke(eventId: Long) = calendar.deleteEvent(eventId)
}
