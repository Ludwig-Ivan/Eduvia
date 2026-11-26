package com.example.librerias.data.repository

import com.example.librerias.data.MeetingSampleData
import com.example.librerias.data.SchoolSampleData
import com.example.librerias.domain.repository.GroupRepository
import com.example.librerias.domain.repository.MeetingRepository
import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class MeetingRepositoryImpl(private val groupRepository: GroupRepository) : MeetingRepository {

    private val meetings = MutableStateFlow(MeetingSampleData.meetings)

    // meetingId -> (studentId -> respuesta). Una respuesta por alumno.
    private val responses = MutableStateFlow(seedResponses())

    private var nextId = (MeetingSampleData.meetings.maxOfOrNull { it.id } ?: 0L) + 1

    /** Los datos de muestra asumen: familia en posición i = alumno con id i + 1. */
    private fun seedResponses(): Map<Long, Map<Long, MeetingResponse>> =
        SchoolSampleData.attendanceByMeeting.mapValues { (_, families) ->
            families.mapIndexed { i, f -> (i + 1).toLong() to f.response }.toMap()
        }

    override fun observeMeetings(): Flow<List<Meeting>> =
        meetings.map { list -> list.distinctBy { it.id } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAttendance(meetingId: Long): Flow<List<FamilyResponse>> =
        meetings
            .map { list -> list.firstOrNull { it.id == meetingId }?.groupId }
            .distinctUntilChanged()
            .flatMapLatest { groupId ->
                if (groupId == null) flowOf(emptyList())
                else combine(
                    groupRepository.observeStudentsOfGroup(groupId),
                    responses
                ) { students, all ->
                    val byStudent = all[meetingId].orEmpty()
                    students.map { s ->
                        FamilyResponse(
                            familyName = "Familia de ${s.name}",
                            response = byStudent[s.id] ?: MeetingResponse.PENDING,
                            studentId = s.id
                        )
                    }
                }
            }

    override fun observeResponses(studentId: Long): Flow<Map<Long, MeetingResponse>> =
        responses.map { all ->
            all.mapNotNull { (meetingId, byStudent) ->
                byStudent[studentId]?.let { meetingId to it }
            }.toMap()
        }

    override suspend fun addMeeting(meeting: Meeting): Meeting {
        val saved = meeting.copy(id = nextId++)
        meetings.update { list -> if (list.any { it.id == saved.id }) list else list + saved }
        // Sin respuestas todavía: todos los alumnos del grupo salen como "pendiente".
        return saved
    }

    override suspend fun updateMeeting(meeting: Meeting): Meeting {
        meetings.update { list -> list.map { if (it.id == meeting.id) meeting else it } }
        return meeting
    }

    override suspend fun setResponse(meetingId: Long, studentId: Long, response: MeetingResponse) {
        responses.update { all ->
            val current = all[meetingId].orEmpty()
            all + (meetingId to (current + (studentId to response)))
        }
    }

    override suspend fun remindPending(meetingId: Long): Int =
        observeAttendance(meetingId).first().count { it.response == MeetingResponse.PENDING }

    override suspend fun deleteMeeting(meetingId: Long) {
        meetings.update { list -> list.filter { it.id != meetingId } }
        responses.update { it - meetingId }
    }
}