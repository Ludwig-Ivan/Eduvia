package com.example.librerias.model

/** Materia escolar asociada a una tarea. `label` es el texto legible que usa la UI. */
enum class Subject(val label: String) {
    MATEMATICAS("Matemáticas"),
    ESPANOL("Español"),
    CIENCIAS("Ciencias"),
    HISTORIA("Historia"),
    OTRA("Otra");

    companion object {
        /** Resuelve la materia desde el texto de la UI; devuelve `OTRA` si no coincide. */
        fun fromLabel(label: String): Subject =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTRA
    }
}