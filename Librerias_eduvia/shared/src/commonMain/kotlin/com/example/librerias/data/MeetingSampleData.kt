package com.example.librerias.data

import com.example.librerias.model.CalendarDate
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse

/**
 * Datos de ejemplo de la pantalla Juntas (Octubre 2026).
 *
 * Solo para la demo, con "hoy = 12 de octubre de 2026" como referencia; el backend usa la
 * fecha real del sistema (OPEN-QUESTIONS Q7). Los valores `isNext` y `relativeLabel` son
 * ilustrativos: en la app real los calcula `ObserveMeetingsUseCase` y no se persisten.
 */
object MeetingSampleData {

    val meetings: List<Meeting> = listOf(
        Meeting(
            id = 1,
            groupId = 1L, // 👈 NUEVO: Asignamos al grupo de Laura
            title = "Junta de padres",
            date = CalendarDate(2026, 10, 15),
            time = "8:00 am",
            location = "Salón 3B",
            hasVirtualLink = true,
            isNext = true,
            relativeLabel = "En 3 días"
        ),
        Meeting(
            id = 2,
            groupId = 1L, // 👈 NUEVO: Asignamos al grupo de Laura
            title = "Entrega de boletas",
            date = CalendarDate(2026, 10, 29),
            time = "9:00 am",
            location = "Dirección",
            response = MeetingResponse.ATTENDING
        )
    )
}