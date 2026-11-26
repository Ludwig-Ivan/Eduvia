package com.example.librerias.data

import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType.EXAM
import com.example.librerias.model.CalendarEventType.MEETING
import com.example.librerias.model.CalendarEventType.TASK

/**
 * Datos de ejemplo: reproducen los puntos de colores del diseño (Octubre 2026).
 *
 * Solo alimentan la demo; el "hoy" de referencia de los ejemplos es el 12 de octubre de 2026,
 * mientras que el backend usa la fecha real del sistema para sus validaciones
 * (OPEN-QUESTIONS Q7).
 */
object CalendarSampleData {

    private fun d(day: Int) = CalendarDate(2026, 10, day)

    /** Agenda ficticia para los puntos del calendario; no procede de la API. */
    val events: List<CalendarEvent> = buildList {
        // JUNTAS
        add(CalendarEvent(id = 1L, groupId = 1L, date = d(15), type = MEETING, title = "Junta de padres", subtitle = "8:00 am · Salón 3B"))
        add(CalendarEvent(id = 2L, groupId = 1L, date = d(29), type = MEETING, title = "Entrega de boletas", subtitle = "9:00 am · Dirección"))

        // TAREAS
        add(CalendarEvent(id = 1L, groupId = 1L, date = d(12), type = TASK, title = "Resolver página 45", subtitle = "Matemáticas · Pendiente"))
        add(CalendarEvent(id = 2L, groupId = 1L, date = d(13), type = TASK, title = "Ciclo del agua", subtitle = "Ciencias · Pendiente"))
        add(CalendarEvent(id = 3L, groupId = 1L, date = d(15), type = TASK, title = "Lectura capítulo 4", subtitle = "Español · Pendiente"))
        add(CalendarEvent(id = 4L, groupId = 1L, date = d(8), type = TASK, title = "Mapa de México", subtitle = "Historia · Entregada"))

        // Las demás tareas de relleno
        var idRelleno = 100L
        listOf(2, 5, 6, 9, 14, 16, 19, 20, 22, 23, 26, 27, 30).forEach { day ->
            add(CalendarEvent(id = idRelleno++, groupId = 1L, date = d(day), type = TASK, title = "Tarea del día $day", subtitle = "Pendiente"))
        }

        // EXÁMENES
        add(CalendarEvent(id = 50L, groupId = 1L, date = d(7), type = EXAM, title = "Examen de matemáticas", subtitle = "10:00 am · Salón 2A"))
        add(CalendarEvent(id = 51L, groupId = 1L, date = d(21), type = EXAM, title = "Examen de ciencias", subtitle = "10:00 am · Salón 2A"))
    }
}