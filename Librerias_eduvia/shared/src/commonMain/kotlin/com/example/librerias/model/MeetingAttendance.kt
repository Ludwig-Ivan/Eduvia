package com.example.librerias.model

/**
 * Respuesta de una familia a una junta (lo ve el maestro).
 *
 * `studentId` asocia la respuesta con el alumno del padre/tutor.
 */
data class FamilyResponse(
    val familyName: String,
    val response: MeetingResponse,
    val studentId: Long = 0L
)

/**
 * Resumen derivado de las respuestas de las familias.
 *
 * `AttendanceSummary` no se persistía como entidad en la app original: se calcula al contar
 * respuestas `ATTENDING`, `DECLINED` y `PENDING`.
 */
fun List<FamilyResponse>.toSummary(): AttendanceSummary = AttendanceSummary(
    confirmed = count { it.response == MeetingResponse.ATTENDING },
    declined = count { it.response == MeetingResponse.DECLINED },
    pending = count { it.response == MeetingResponse.PENDING }
)

/**
 * Lo que captura el maestro en CreateMeetingScreen.
 *
 * `dateText` se transforma a ISO `YYYY-MM-DD` en la capa de red antes de enviarlo a la API.
 */
data class NewMeetingInput(
    val title: String,
    val dateText: String,
    val time: String,
    val place: String,
    val link: String,
    val reminder: Boolean,
    val groupId: Long = 1L
)