package com.example.librerias.api.domain

import java.io.Serializable

/**
 * Capa: dominio (clave primaria compuesta de una entidad JPA).
 *
 * Responsabilidad: identificar de forma única la entrega de una tarea (`SchoolTask`) por parte
 * de un alumno (`Student`). Corresponde a la PK `(task_id, student_id)` de la tabla
 * `task_deliveries`.
 *
 * Colaboradores: la entidad `TaskDelivery`, que declara `@IdClass(TaskDeliveryId::class)`, y
 * `TaskDeliveryRepository`, que lo usa como tipo de identificador.
 *
 * Por qué existe: garantiza la idempotencia de `POST /api/v1/tasks/{id}/deliveries` (marcar dos
 * veces la misma entrega no crea dos filas) y permite borrar de una sola vez todas las entregas
 * de una tarea cuando se elimina. Implementa [Serializable] porque JPA lo exige para las claves
 * compuestas.
 */
data class TaskDeliveryId(
    var task: Long = 0,
    var student: Long = 0,
) : Serializable

/**
 * Capa: dominio (clave primaria compuesta de una entidad JPA).
 *
 * Responsabilidad: identificar la respuesta de un alumno (`Student`) a una junta (`Meeting`).
 * Corresponde a la PK `(meeting_id, student_id)` de la tabla `meeting_responses`.
 *
 * Colaboradores: la entidad `MeetingResponseEntity` (`@IdClass(MeetingResponseId::class)`), el
 * enum `MeetingResponse` y `MeetingResponseRepository`.
 *
 * Por qué existe: hace idempotente `PUT /api/v1/meetings/{id}/responses/{studentId}` —una misma
 * familia puede cambiar su respuesta, pero no duplicarla— y permite consultar o borrar todas las
 * respuestas de una junta. `Serializable` es un requisito de JPA para las claves compuestas.
 */
data class MeetingResponseId(
    var meeting: Long = 0,
    var student: Long = 0,
) : Serializable
