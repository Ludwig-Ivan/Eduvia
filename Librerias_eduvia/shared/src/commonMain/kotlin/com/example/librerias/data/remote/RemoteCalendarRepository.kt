package com.example.librerias.data.remote

import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.model.AppFailureException
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Calendario remoto. Las tareas y juntas crean su evento en el backend dentro de la misma
 * transacción, así que desde la app solo se envía un POST explícito para eventos EXAM.
 *
 * Implementa [CalendarRepository] sobre HTTP (C1-C3), mapeando `CalendarEventDto` a [CalendarEvent]
 * con `toModel`. La caché [RemoteListStore] está separada por rol: la vista del docente recibe
 * `attendance` y la del padre no. Las operaciones que van a la red propagan `ApiException`; los
 * fallos al cargar el flujo se ignoran y conservan el último listado conocido.
 */
class RemoteCalendarRepository(
    private val api: ApiClient,
    reporter: RemoteErrorReporter? = null,
) : CalendarRepository {

    private val byRole = RemoteListStore<UserRole, CalendarEvent>(reporter)

    /**
     * `GET calendar/events?role=&page=&size=` (C1): mapea `PageDto<CalendarEventDto>` a
     * [CalendarEvent] y cachea el resultado por [UserRole]. Solo el primer suscriptor de cada rol
     * descarga; si falla, se conserva el último valor cacheado.
     */
    override fun observeEvents(role: UserRole): Flow<List<CalendarEvent>> =
        byRole.flow(role) {
            api.get<PageDto<CalendarEventDto>>("calendar/events?role=${role.name}&page=0&size=100")
                .items.map { it.toModel() }
        }

    /**
     * Para EXAM, `POST calendar/events` (C2): envía [CreateCalendarEventRequestDto], mapea el DTO a
     * [CalendarEvent] y lo inserta en la caché de ambos roles (sin asistencia para el padre). Para
     * TASK y MEETING el backend ya creó el evento con la tarea/junta, así que solo se actualiza la
     * caché local. Propaga errores de la API en el caso EXAM.
     */
    override suspend fun addEvent(event: CalendarEvent) = try {
        if (event.type == CalendarEventType.EXAM) {
            val dto: CalendarEventDto = api.post("calendar/events", event.toCreateRequest())
            upsert(dto.toModel())
        } else {
            // TASK y MEETING: el evento ya existe en el backend, solo refrescamos la caché local.
            upsert(event)
        }
    } catch (e: Exception) {
        throw AppFailureException(api.errorOf(e))
    }

    /**
     * `DELETE calendar/events/{id}` (C3, sin cuerpo): tras el borrado remoto retira el evento de las
     * cachés de todos los roles. Propaga errores de la API.
     */
    override suspend fun deleteEvent(eventId: Long) = try {
        api.deleteNoContent("calendar/events/$eventId")
        removeLocalEvent(eventId)
    } catch (e: Exception) {
        throw AppFailureException(api.errorOf(e))
    }

    /** Recarga todos los listados de eventos cacheados (lo usan los repositorios de juntas y tareas). */
    suspend fun refreshEvents() = byRole.refreshAll()

    /** Retira un evento de todas las cachés por rol, sin llamar a la red. */
    suspend fun removeLocalEvent(eventId: Long) = byRole.removeWhereAll { it.id == eventId }

    private suspend fun upsert(event: CalendarEvent) {
        byRole.update(UserRole.TEACHER) { current ->
            if (current.any { it.id == event.id && it.type == event.type }) current else current + event
        }
        byRole.update(UserRole.PARENT) { current ->
            val parentEvent = event.copy(attendance = null)
            if (current.any { it.id == parentEvent.id && it.type == parentEvent.type }) current else current + parentEvent
        }
    }
}
