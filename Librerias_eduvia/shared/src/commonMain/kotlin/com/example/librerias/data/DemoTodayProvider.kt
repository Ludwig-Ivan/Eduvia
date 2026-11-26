
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.librerias.data

import com.example.librerias.domain.util.TodayProvider
import com.example.librerias.model.CalendarDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Proveedor de la fecha "hoy" y de la hora actual para la app de demostración.
 *
 * Los datos de ejemplo toman como referencia un "hoy" fijo en 12 de octubre de 2026
 * (ver `SchoolSampleData`), mientras que el backend usa la fecha real del sistema para sus
 * validaciones (OPEN-QUESTIONS Q7). Esta implementación consulta el reloj del dispositivo,
 * sin alterar el comportamiento existente.
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
