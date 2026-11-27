
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.librerias.data

import com.example.librerias.domain.util.TodayProvider
import com.example.librerias.model.CalendarDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Proveedor de la fecha "hoy" y de la hora actual.
 *
 * Consulta el reloj del dispositivo (la misma fecha real que usa el backend para sus
 * validaciones, OPEN-QUESTIONS Q7); no fija ninguna fecha de ejemplo.
 */
class DemoTodayProvider : TodayProvider {

    private fun now() = kotlin.time.Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    override fun today(): CalendarDate {
        val n = now()
        return CalendarDate(n.year, n.monthNumber, n.dayOfMonth)
    }

    override fun nowMinutes(): Int {
        val n = now()
        return n.hour * 60 + n.minute
    }
}
