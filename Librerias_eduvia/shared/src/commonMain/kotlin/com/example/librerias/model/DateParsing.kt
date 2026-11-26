/**
 * Lectura de fechas introducidas por el usuario.
 *
 * El modelo usa `CalendarDate` (año, mes y día), mientras que el contrato HTTP transporta las
 * fechas como texto ISO `YYYY-MM-DD`; la conversión entre ambos la hace `data/remote/Mappers.kt`.
 * Este archivo solo interpreta el texto de los formularios (`15/10/2026`, `15 oct`, etc.) para
 * construir un `CalendarDate`, no genera el formato de la API.
 */
package com.example.librerias.model

private val shortMonths = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
)

/**
 * Convierte el texto que escribe el usuario en un CalendarDate.
 * Acepta: "15/10/2026", "15-10-2026", "Vie 15 oct", "15 oct", "Viernes 15 de octubre".
 * Si no trae año usa [defaultYear]. Devuelve null si no es una fecha válida.
 */
fun parseDateOrNull(text: String, defaultYear: Int): CalendarDate? {
    val clean = text.trim().lowercase()

    val numeric = clean.split('/', '-')
    if (numeric.size == 3) {
        val d = numeric[0].trim().toIntOrNull()
        val m = numeric[1].trim().toIntOrNull()
        val y = numeric[2].trim().toIntOrNull()
        if (d != null && m != null && y != null) {
            return CalendarDate(y, m, d).takeIf { it.isValid() }
        }
    }

    val words = clean.split(' ').filter { it.isNotBlank() && it != "de" }
    val dayIndex = words.indexOfFirst { it.toIntOrNull() != null }
    if (dayIndex == -1 || dayIndex + 1 >= words.size) return null

    val day = words[dayIndex].toInt()
    val monthWord = words[dayIndex + 1]
    val month = shortMonths.indexOfFirst { monthWord.startsWith(it) } + 1
    if (month == 0) return null

    return CalendarDate(defaultYear, month, day).takeIf { it.isValid() }
}