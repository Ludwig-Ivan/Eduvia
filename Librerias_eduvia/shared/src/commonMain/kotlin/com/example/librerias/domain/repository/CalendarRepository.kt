package com.example.librerias.domain.repository

import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Capa de dominio: eventos de calendario vistos por rol, sin depender del origen de datos.
 *
 * Lo satisfacen `CalendarRepositoryImpl` (listas en memoria para la demo) y
 * `RemoteCalendarRepository` (HTTP `calendar/events`, con caché por rol). El contrato de API
 * crea el evento de una tarea o junta en la misma operación que la entidad: por eso `TASK` y
 * `MEETING` solo refrescan la caché local al pasar por `addEvent`, y solo `EXAM` hace un POST.
 */
interface CalendarRepository {
    /**
     * Observa los eventos visibles para [role].
     * El maestro ve resumen de asistencia; el padre ve su propia vista.
     * @return flujo con la lista de eventos del rol indicado.
     */
    fun observeEvents(role: UserRole): Flow<List<CalendarEvent>>

    /**
     * Añade o actualiza un evento en la caché; si [event] no trae id, el repositorio se lo asigna.
     * @return sin valor; los observadores reciben el evento nuevo.
     */
    suspend fun addEvent(event: CalendarEvent)                  // asigna el id

    /**
     * Elimina el evento [eventId] (en remoto, DELETE `calendar/events/{id}` y se quita de la caché).
     * @return sin valor.
     */
    suspend fun deleteEvent(eventId: Long)
}
