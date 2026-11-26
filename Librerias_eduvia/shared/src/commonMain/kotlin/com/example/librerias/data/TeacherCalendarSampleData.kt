package com.example.librerias.data

import com.example.librerias.model.AttendanceSummary
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType.EXAM
import com.example.librerias.model.CalendarEventType.MEETING
import com.example.librerias.model.CalendarEventType.TASK

/**
 * Calendario de ejemplo de la vista del maestro (octubre de 2026), con colores y resúmenes
 * de asistencia para la demo.
 *
 * "Hoy" en los ejemplos es el 12 de octubre de 2026; el backend usa la fecha real del sistema
 * (OPEN-QUESTIONS Q7). Los `AttendanceSummary` son derivados de las respuestas de las familias.
 */
object TeacherCalendarSampleData {

    private const val GROUP_SIZE = 28

    private fun d(day: Int) = CalendarDate(2026, 10, day)

    val events: List<CalendarEvent> = buildList {
        var id = 1L

        // Juntas (naranja)
        add(
            CalendarEvent(
                id = id++,
                groupId = 1L,
                date = d(15),
                type = MEETING,
                title = "Junta de padres",
                subtitle = "8:00 am · Salón 3B",
                attendance = AttendanceSummary(confirmed = 18, declined = 8, pending = 4)
            )
        )
        add(
            CalendarEvent(
                id = id++,
                groupId = 1L,
                date = d(29),
                type = MEETING,
                title = "Junta de fin de mes",
                subtitle = "9:00 am · Dirección",
                attendance = AttendanceSummary(confirmed = 2, declined = 0, pending = 28)
            )
        )

        // Tareas (azul)
        add(CalendarEvent(
            id = id++,
            groupId = 1L,
            date = d(15),
            type = TASK,
            title = "Lectura capítulo 4",
            subtitle = "Español · Entregadas 22 de $GROUP_SIZE"
        ))

        listOf(2, 5, 6, 8, 9, 12, 13, 14, 16, 19, 20, 22, 23, 26, 27, 30).forEach { day ->
            val delivered = when {
                day < 12 -> GROUP_SIZE
                day == 12 -> 22
                else -> 0
            }
            add(CalendarEvent(
                id = id++,
                groupId = 1L,
                date = d(day),
                type = TASK,
                title = "Tarea del día $day",
                subtitle = "Entregadas $delivered de $GROUP_SIZE"
            ))
        }

        // Exámenes (morado)
        add(CalendarEvent(
            id = id++,
            groupId = 1L,
            date = d(7),
            type = EXAM,
            title = "Examen de matemáticas",
            subtitle = "10:00 am · Salón 2A"
        ))
        add(CalendarEvent(
            id = id++,
            groupId = 1L,
            date = d(21),
            type = EXAM,
            title = "Examen de ciencias",
            subtitle = "10:00 am · Salón 2A"
        ))
    }
}