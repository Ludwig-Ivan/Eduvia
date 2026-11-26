package com.example.librerias.domain.repository

import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import kotlinx.coroutines.flow.Flow

/**
 * Capa de dominio: juntas escolares, asistencia derivada y respuestas de las familias.
 *
 * Lo satisfacen `MeetingRepositoryImpl` (en memoria: alumnos del grupo más un mapa de respuestas)
 * y `RemoteMeetingRepository` (HTTP: `meetings`, `meetings/{id}`, `meetings/{id}/attendance`,
 * `meetings/responses`, `meetings/{id}/responses/{studentId}` y `meetings/{id}/reminders`).
 * La interfaz refleja el contrato de API sin exponer DTO ni detalles de red.
 */
interface MeetingRepository {
    /**
     * Observa todas las juntas.
     * @return flujo con la lista de juntas.
     */
    fun observeMeetings(): Flow<List<Meeting>>

    /**
     * Observa la asistencia de [meetingId]: se deriva de los alumnos del grupo y de sus respuestas.
     * @return flujo de respuestas por familia; sin respuesta se muestra `PENDING`.
     */
    fun observeAttendance(meetingId: Long): Flow<List<FamilyResponse>>

    /**
     * meetingId -> respuesta de ESE alumno. Es un mapa de respuestas por junta, no una lista:
     * cada entrada representa la respuesta de [studentId] en una junta distinta.
     * @return flujo con el mapa `id de junta -> MeetingResponse` de ese alumno.
     */
    fun observeResponses(studentId: Long): Flow<Map<Long, MeetingResponse>>

    /**
     * Guarda una junta nueva; el repositorio le asigna el id.
     * @return la junta ya persistida con su id real.
     */
    suspend fun addMeeting(meeting: Meeting): Meeting

    /**
     * Actualiza una junta existente conservando su id.
     * @return la junta actualizada.
     */
    suspend fun updateMeeting(meeting: Meeting): Meeting

    /**
     * Registra o reemplaza la respuesta de [studentId] en [meetingId].
     * @return sin valor; la asistencia derivada se recalcula sola.
     */
    suspend fun setResponse(meetingId: Long, studentId: Long, response: MeetingResponse)

    /**
     * Cuenta las familias que siguen pendientes en [meetingId] para el recordatorio.
     * @return número de respuestas `PENDING` (0 si ya respondieron todas).
     */
    suspend fun remindPending(meetingId: Long): Int

    /**
     * Elimina la junta [meetingId] y sus respuestas asociadas.
     * @return sin valor.
     */
    suspend fun deleteMeeting(meetingId: Long)
}
