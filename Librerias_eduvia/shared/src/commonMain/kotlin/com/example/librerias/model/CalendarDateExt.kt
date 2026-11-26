/**
 * Utilidades para comparar y etiquetar fechas `CalendarDate` en la UI.
 *
 * El contrato HTTP usa texto ISO `YYYY-MM-DD`, por eso `data/remote/Mappers.kt` convierte
 * entre ese formato y `CalendarDate`; aquí solo se valida, compara y formatea el modelo,
 * sin serialización ni persistencia.
 */
package com.example.librerias.model
// expense
/** Valida el rango de mes y día; se aplica al interpretar fechas de la UI o del contrato HTTP. */
fun CalendarDate.isValid(): Boolean =
    month in 1..12 && day in 1..daysInMonth(year, month)

/** Días desde 1970-01-01 (algoritmo days_from_civil, Kotlin puro). */
fun CalendarDate.toEpochDay(): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (month + 9) % 12 // marzo = 0
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Días entre esta fecha y otra; positivo si `other` es posterior. */
fun CalendarDate.daysUntil(other: CalendarDate): Int =
    (other.toEpochDay() - toEpochDay()).toInt()

/** Comparación de fechas usando días desde el epoch, sin depender de zonas horarias. */
fun CalendarDate.isBefore(other: CalendarDate): Boolean =
    toEpochDay() < other.toEpochDay()

/** Ej: "15 de octubre" */
fun CalendarDate.shortLabel(): String = "$day de ${monthNames[month - 1].lowercase()}"

/**
 * Ej: "Hoy", "Mañana", "En 3 días", "Hace 2 días".
 *
 * Etiqueta derivada de la diferencia con `today`; no se persistía en la app original.
 */
fun CalendarDate.relativeLabel(today: CalendarDate): String {
    val diff = today.daysUntil(this)
    return when {
        diff == 0 -> "Hoy"
        diff == 1 -> "Mañana"
        diff == -1 -> "Ayer"
        diff > 1 -> "En $diff días"
        else -> "Hace ${-diff} días"
    }
}