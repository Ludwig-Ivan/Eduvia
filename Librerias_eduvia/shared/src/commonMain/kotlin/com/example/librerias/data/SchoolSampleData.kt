package com.example.librerias.data

import com.example.librerias.model.CalendarDate
import com.example.librerias.model.FamilyResponse
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.Student
import com.example.librerias.model.Subject
import com.example.librerias.model.User
import com.example.librerias.model.UserRole

/**
 * Datos de ejemplo de usuarios, grupos, alumnos, tareas y asistencia. Solo para demo.
 *
 * Las fechas están pensadas para un "hoy" fijo en 12 de octubre de 2026; el backend, en cambio,
 * valida con la fecha real del sistema (OPEN-QUESTIONS Q7). La ruta activa de la app consume la
 * API remota: estos objetos se conservan como ejemplo de la demo.
 */
object SchoolSampleData {

    private const val GROUP_SIZE = 28

    private fun d(day: Int) = CalendarDate(2026, 10, day)
    private fun ids(range: IntRange): Set<Long> = range.map { it.toLong() }.toSet()

    // Cuentas de prueba: (usuario, contraseña)
    val accounts: List<Pair<User, String>> = listOf(
        User(1L, "Laura Hernández", "laura@eduvia.mx", "5512345678", UserRole.TEACHER) to "Maestra123",
        User(2L, "María Pérez", "maria@correo.com", "5598765432", UserRole.PARENT) to "Padre1234"
    )
    val users: List<User> = accounts.map { it.first }

    val groups: List<SchoolGroup> = listOf(
        SchoolGroup(1L, "3° B", "Escuela Primaria Benito Juárez", "Matemáticas", "3B-7K2P", 1L, GROUP_SIZE),
        SchoolGroup(2L, "2° A", "Escuela Primaria Benito Juárez", "", "2A-9M4X", 1L, 0),
        // 👇 GRUPO DE PEDRO (2° B) CON 2 ALUMNOS
        //SchoolGroup(4L, "2° B", "Secundaria Técnica", "Geografía", "2B-XXXX", 3L, 2)
    )

    // Alumno 1 = hijo/a de María (parentId = 2) + Alumnos de Carlos en 2° B
    val students: List<Student> = List(GROUP_SIZE) { i ->
        Student(
            id = (i + 1).toLong(),
            name = if (i == 0) "Sofía Pérez" else "Alumno ${i + 1}",
            groupId = 1L,
            parentId = if (i == 0) 2L else 0L
        )
    } + listOf(
        // 👇 HIJOS DE CARLOS EN EL GRUPO DE PEDRO (2° B)
        Student(id = 100L, name = "Luis Gómez", groupId = 4L, parentId = 3L),
        Student(id = 101L, name = "Ana Gómez", groupId = 4L, parentId = 3L)
    )

    private fun task(
        id: Long,
        subject: Subject,
        title: String,
        description: String,
        due: CalendarDate,
        published: CalendarDate,
        delivered: Set<Long>,
        hasPhoto: Boolean = false
    ) = SchoolTask(
        id = id,
        groupId = 1L,
        title = title,
        subject = subject,
        description = description,
        dueDate = due,
        publishedDate = published,
        teacherName = "Laura",
        totalStudents = GROUP_SIZE,
        hasPhoto = hasPhoto,
        deliveredStudentIds = delivered
    )

    // "Hoy" en la demo = 12 de octubre de 2026
    val tasks: List<SchoolTask> = listOf(
        task(1L, Subject.MATEMATICAS, "Resolver página 45",
            "Ejercicios 1 al 10. Traer regla y lápiz de colores para la actividad del final.",
            d(12), d(9), ids(2..23), hasPhoto = true),
        task(2L, Subject.CIENCIAS, "Ciclo del agua", "Dibujo con foto.",
            d(13), d(9), ids(2..15)),
        task(3L, Subject.ESPANOL, "Lectura cap. 4", "Leer el capítulo 4 y escribir un resumen.",
            d(15), d(10), ids(2..4)),
        task(4L, Subject.HISTORIA, "Mapa de México", "Mapa con los estados y sus capitales.",
            d(8), d(5), ids(1..23)),                       // la entregó el alumno 1
        task(5L, Subject.MATEMATICAS, "Pág. 38", "Problemas de la página 38.",
            d(6), d(2), ids(1..28)),                       // completa
        task(6L, Subject.ESPANOL, "Lectura cap. 3", "Resumen del capítulo 3.",
            d(2), CalendarDate(2026, 9, 28), ids(2..27))   // el alumno 1 no la entregó → vencida
    )

    private val familyNames = listOf(
        "Ramírez", "Torres", "Núñez", "Ortega", "García", "Hernández", "López", "Martínez",
        "González", "Pérez", "Sánchez", "Rivera", "Flores", "Gómez", "Díaz", "Cruz",
        "Morales", "Reyes", "Gutiérrez", "Ruiz", "Mendoza", "Aguilar", "Castillo", "Vargas",
        "Romero", "Herrera", "Medina", "Castro", "Jiménez", "Vázquez"
    )

    /** 30 familias: las primeras [confirmed] asisten, las siguientes [declined] no pueden, el resto sin responder. */
    fun familiesFor(confirmed: Int, declined: Int): List<FamilyResponse> =
        familyNames.mapIndexed { i, name ->
            val response = when {
                i < confirmed -> MeetingResponse.ATTENDING
                i < confirmed + declined -> MeetingResponse.DECLINED
                else -> MeetingResponse.PENDING
            }
            FamilyResponse("Familia $name", response)
        }

    // Mismos números que MeetingSampleData / TeacherCalendarSampleData
    val attendanceByMeeting: Map<Long, List<FamilyResponse>> = mapOf(
        1L to familiesFor(confirmed = 18, declined = 8),
        2L to familiesFor(confirmed = 2, declined = 0)
    )
}