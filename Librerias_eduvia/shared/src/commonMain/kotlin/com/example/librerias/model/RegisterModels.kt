package com.example.librerias.model

/**
 * Rol del usuario al registrarse.
 *
 * Determina qué pantallas y grupos de endpoints consume la app: `PARENT` (padre o tutor) o
 * `TEACHER` (maestro/a).
 */
enum class UserRole {
    PARENT,   // Padre o tutor
    TEACHER   // Maestro/a
}

/**
 * Datos de un hijo/a vinculado a la cuenta del padre.
 *
 * `classCode` es el código del grupo del maestro y `studentName` el nombre que se mostrará
 * al seleccionar al alumno en las pantallas del padre.
 */
data class ChildInfo(
    val classCode: String = "",
    val studentName: String = ""
)