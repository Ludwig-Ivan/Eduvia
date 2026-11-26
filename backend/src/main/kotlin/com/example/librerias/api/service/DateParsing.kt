package com.example.librerias.api.service

import com.example.librerias.api.web.badRequest
import com.example.librerias.api.web.dto.FieldErrorDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Parsea fechas del contrato (`YYYY-MM-DD`) con el mismo error para juntas y tareas. */
internal fun parseDate(text: String): LocalDate = try {
    LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: DateTimeParseException) {
    throw badRequest("Fecha inválida, use YYYY-MM-DD", listOf(FieldErrorDto("dateText", "Formato YYYY-MM-DD")))
}
