package com.example.librerias.model

/**
 * Usuario con sesión (padre/tutor o maestro). El rol es UserRole (ya lo tienes).
 *
 * `id`, `fullName`, `email`, `phone` y `role` son datos de la cuenta. `firstName` no era un
 * campo persistido en la app original: se deriva de `fullName` cada vez que se lee.
 */
data class User(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: UserRole
) {
    /** Nombre derivado del primer token de `fullName`; no se guardaba como campo propio. */
    val firstName: String get() = fullName.trim().substringBefore(' ')
}

/**
 * Alumno vinculado a un grupo y a un padre/tutor.
 *
 * `groupId` apunta al grupo del maestro y `parentId` a la cuenta del padre/tutor.
 */
data class Student(
    val id: Long,
    val name: String,
    val groupId: Long,
    val parentId: Long
)