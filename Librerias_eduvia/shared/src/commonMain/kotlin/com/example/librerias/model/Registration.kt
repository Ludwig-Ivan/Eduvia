package com.example.librerias.model

/**
 * Datos del formulario de registro. `children` se rellena para `PARENT` y `groups` para
 * `TEACHER`; `confirmPassword` y `acceptedTerms` solo validan el formulario.
 */
data class RegisterInput(
    val role: UserRole,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val acceptedTerms: Boolean,
    val children: List<ChildInfo> = emptyList(),  // si es PARENT
    val groups: List<GroupInput> = emptyList()    // si es TEACHER
)

/**
 * Para GroupsReadyScreen: los grupos recién creados con sus códigos.
 *
 * `user` es la cuenta ya registrada y `createdGroups` contiene los códigos que el maestro
 * comparte con las familias.
 */
data class RegistrationResult(
    val user: User,
    val createdGroups: List<SchoolGroup>
)