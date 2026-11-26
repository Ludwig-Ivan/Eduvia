package com.example.librerias.model

/** Respuesta del padre/tutor a una junta: sin responder, asistirá o no asistirá. */
enum class MeetingResponse { PENDING, ATTENDING, DECLINED }

/**
 * Junta de padres programada por el maestro para un grupo.
 *
 * `date` es un `CalendarDate`; en el contrato HTTP viaja como texto ISO `YYYY-MM-DD` y la
 * conversión la realiza `data/remote/Mappers.kt`. `time` y `location` se muestran tal cual.
 *
 * `isNext` y `relativeLabel` son campos derivados calculados por `ObserveMeetingsUseCase`
 * a partir de la fecha de hoy; no se persistían en la app original. `dateTimeLabel()` también
 * es una etiqueta derivada solo para la UI.
 */
data class Meeting(
    val id: Long,
    val groupId: Long = 1L,
    val title: String,
    val date: CalendarDate,
    val time: String,                  // Ej: "8:00 am"
    val location: String,              // Ej: "Salón 3B"
    val hasVirtualLink: Boolean = false,
    val isNext: Boolean = false,       // lo calcula ObserveMeetingsUseCase
    val relativeLabel: String? = null, // lo calcula ObserveMeetingsUseCase
    val response: MeetingResponse = MeetingResponse.PENDING,
    val virtualLink: String? = null,   // NUEVO
    val reminderEnabled: Boolean = true // NUEVO
)

/**
 * Ej: "Jueves 15 de octubre · 8:00 am".
 *
 * Etiqueta derivada para la UI; no se persistía en la app original.
 */
fun Meeting.dateTimeLabel(): String = "${date.longLabel()} · $time"