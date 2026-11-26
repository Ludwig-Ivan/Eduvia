package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.CalendarRepository
import com.example.librerias.domain.repository.MeetingRepository
import com.example.librerias.domain.util.TodayProvider
import com.example.librerias.model.AppResult
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.MEETING_ID_OFFSET
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Capa de caso de uso: protege la edición de juntas (duplicados y sincronización con el calendario) frente a regresiones en las reglas de dominio. */
class UpdateMeetingUseCaseTest {

    private val original = Meeting(
        id = 3L,
        title = "Junta Fase6",
        date = CalendarDate(2026, 12, 5),
        time = "10:00 am",
        location = "Salon Fase6",
    )

    @Test
    fun `editar solo el titulo no se rechaza como junta duplicada`() = runBlocking {
        val repository = FakeMeetingRepository(original)
        val calendar = FakeCalendarRepository()
        val useCase = UpdateMeetingUseCase(repository, calendar, FixedToday)

        val result = useCase(
            original = original,
            title = "Junta Fase6 editada",
            dateText = "05/12/2026",
            time = "10:00 am",
            place = "Salon Fase6",
            link = "",
            reminder = true,
        )

        val success = assertIs<AppResult.Success<Meeting>>(result)
        assertEquals(3L, success.value.id)
        assertEquals("Junta Fase6 editada", success.value.title)
        assertEquals("Junta Fase6 editada", repository.saved?.title)
        assertEquals(MEETING_ID_OFFSET + 3L, calendar.added?.id)
    }

    private class FakeMeetingRepository(initial: Meeting) : MeetingRepository {
        private val state = MutableStateFlow(listOf(initial))
        var saved: Meeting? = null

        override fun observeMeetings(): Flow<List<Meeting>> = state
        override fun observeAttendance(meetingId: Long): Flow<List<FamilyResponse>> = flowOf(emptyList())
        override fun observeResponses(studentId: Long): Flow<Map<Long, MeetingResponse>> = flowOf(emptyMap())
        override suspend fun addMeeting(meeting: Meeting): Meeting = meeting
        override suspend fun updateMeeting(meeting: Meeting): Meeting {
            saved = meeting
            state.value = listOf(meeting)
            return meeting
        }
        override suspend fun setResponse(meetingId: Long, studentId: Long, response: MeetingResponse) = Unit
        override suspend fun remindPending(meetingId: Long): Int = 0
        override suspend fun deleteMeeting(meetingId: Long) = Unit
    }

    private class FakeCalendarRepository : CalendarRepository {
        var added: CalendarEvent? = null
        override fun observeEvents(role: UserRole): Flow<List<CalendarEvent>> = flowOf(emptyList())
        override suspend fun addEvent(event: CalendarEvent) { added = event }
        override suspend fun deleteEvent(eventId: Long) = Unit
    }

    private object FixedToday : TodayProvider {
        override fun today(): CalendarDate = CalendarDate(2026, 11, 24)
        override fun nowMinutes(): Int = 9 * 60
    }
}
