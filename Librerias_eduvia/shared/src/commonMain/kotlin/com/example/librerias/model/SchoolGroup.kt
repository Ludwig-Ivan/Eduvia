package com.example.librerias.model

/**
 * Grupo del maestro. El código lo comparte con los padres.
 *
 * `id` identifica el grupo, `code` es el valor que los padres introducen para vincular a sus
 * hijos y `studentCount` es el número de alumnos que se muestra en las pantallas del maestro.
 */
data class SchoolGroup(
    val id: Long,
    val name: String,       // "3° B"
    val school: String,
    val subject: String,    // opcional
    val code: String,       // "3B-7K2P"
    val teacherId: Long,
    val studentCount: Int = 0
)

/**
 * Datos que captura el maestro al registrarse. Reemplaza a TeacherGroup de ui/auth.
 *
 * El código de acceso del grupo se genera después del registro y se muestra en GroupsReadyScreen.
 */
data class GroupInput(
    val groupName: String = "",
    val schoolName: String = "",
    val subject: String = ""
)