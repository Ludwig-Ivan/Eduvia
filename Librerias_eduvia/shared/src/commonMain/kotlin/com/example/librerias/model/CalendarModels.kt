package com.example.librerias.model

/**
 * Fecha usada por el modelo: año, mes (1..12) y día (1..31).
 *
 * En el contrato HTTP se representa como texto ISO `YYYY-MM-DD`; `data/remote/Mappers.kt`
 * convierte entre este tipo y esa cadena (`toApiDate` / `toCalendarDate`).
 */
data class CalendarDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
)

/** Tipo de evento del calendario: tarea, junta o examen. */
enum class CalendarEventType { TASK, MEETING, EXAM }

/**
 * Los eventos de junta se identifican en la API como 1000 + meetings.id.
 *
 * El offset evita colisionar con los eventos de tarea, que usan el mismo id que la tarea;
 * la navegación lo resta para recuperar el id de la junta (OPEN-QUESTIONS Q8).
 */
const val MEETING_ID_OFFSET = 1000L

/**
 * Resumen de asistencia a una junta (lo ve el maestro).
 *
 * Se calcula en la app contando las respuestas de las familias; no se persistía como entidad
 * propia en la app original. `total` y `confirmedFraction` también son valores derivados.
 */
data class AttendanceSummary(
    val confirmed: Int,
    val declined: Int,
    val pending: Int
) {
    /** Total de respuestas; derivado de los tres contadores. */
    val total: Int get() = confirmed + declined + pending

    /** Proporción de asistencias confirmadas (0 si no hay respuestas); derivado. */
    val confirmedFraction: Float get() = if (total == 0) 0f else confirmed.toFloat() / total
}

/**
 * Entrada de la agenda del calendario para un grupo.
 *
 * `date` sigue el formato `CalendarDate` y se envía a la API como ISO `YYYY-MM-DD`.
 * Para los eventos `MEETING`, `id` es `MEETING_ID_OFFSET + meetings.id` en el contrato HTTP;
 * `attendance` es un resumen derivado de las respuestas de las familias.
 */
data class CalendarEvent(
    val id: Long,
    val groupId: Long = 1L,
    val date: CalendarDate,
    val type: CalendarEventType,
    val title: String,
    val subtitle: String,
    val attendance: AttendanceSummary? = null
)

// ---------------------------------------------------------------------------
// Utilidades de fecha en Kotlin puro (sin dependencias extra)
// ---------------------------------------------------------------------------

/** Nombres de los meses en español, en el orden en que `CalendarDate.month` va de 1 a 12. */
val monthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** Nombres de los días de la semana en español, empezando por lunes. */
val weekdayNames = listOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

/** Indica si un año es bisiesto; se usa para validar el día de `CalendarDate`. */
fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/** Número de días del mes (1..12) teniendo en cuenta los años bisiestos. */
fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Día de la semana con lunes primero: 0 = lunes ... 6 = domingo (algoritmo de Sakamoto). */
fun weekdayIndexMondayFirst(year: Int, month: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    val sunday0 = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7 // 0 = domingo
    return (sunday0 + 6) % 7
}

/** Ej: "Jueves 15 de octubre" */
fun CalendarDate.longLabel(): String =
    "${weekdayNames[weekdayIndexMondayFirst(year, month, day)]} $day de ${monthNames[month - 1].lowercase()}"