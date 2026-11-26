package com.example.librerias.model

/**
 * Estado derivado de una tarea para un alumno concreto: pendiente, entregada o vencida.
 * [TaskStatus] no se persistía en la app original; `statusFor` lo calcula en cada lectura.
 */
enum class TaskStatus { PENDING, DELIVERED, EXPIRED }

/**
 * Tarea publicada por un maestro para un grupo.
 *
 * `dueDate` y `publishedDate` son `CalendarDate`; en el contrato HTTP se representan como
 * texto ISO `YYYY-MM-DD` y la conversión la hace `data/remote/Mappers.kt`.
 *
 * Son campos derivados que no se persistían en la app original: `displayTitle`,
 * `deliveredCount`, `missingCount`, `isComplete` y `deliveredFraction`. Tampoco se guardaban
 * los resultados de `isDeliveredBy`, `isActive` ni `statusFor`: todos se calculan a partir de
 * `deliveredStudentIds`, `totalStudents` y la fecha de hoy.
 */
data class SchoolTask(
    val id: Long,
    val groupId: Long,
    val title: String,
    val subject: Subject,
    val description: String,
    val dueDate: CalendarDate,
    val publishedDate: CalendarDate,
    val teacherName: String,
    val totalStudents: Int,
    val hasPhoto: Boolean = false,
    val deliveredStudentIds: Set<Long> = emptySet()
) {
    /** Título compuesto para la lista; derivado, no persistido en la app original. */
    val displayTitle: String get() = "${subject.label} · $title"

    /** Número de alumnos que entregaron; derivado de `deliveredStudentIds`. */
    val deliveredCount: Int get() = deliveredStudentIds.size

    /** Alumnos que faltan por entregar; derivado, nunca se guardó como campo. */
    val missingCount: Int get() = (totalStudents - deliveredCount).coerceAtLeast(0)

    /** Indica si entregaron todos; derivado de `totalStudents` y `deliveredCount`. */
    val isComplete: Boolean get() = totalStudents > 0 && deliveredCount >= totalStudents

    /** Proporción de entregas (0 cuando no hay alumnos); derivado, no persistido. */
    val deliveredFraction: Float
        get() = if (totalStudents == 0) 0f else deliveredCount.toFloat() / totalStudents

    /** Comprueba la entrega de un alumno a partir del conjunto en memoria. */
    fun isDeliveredBy(studentId: Long): Boolean = studentId in deliveredStudentIds

    /** Una tarea está activa mientras su fecha de entrega no sea anterior a hoy. */
    fun isActive(today: CalendarDate): Boolean = !dueDate.isBefore(today)

    /** Estado derivado para un alumno; no se almacenaba en la app original. */
    fun statusFor(today: CalendarDate, studentId: Long): TaskStatus = when {
        isDeliveredBy(studentId) -> TaskStatus.DELIVERED
        dueDate.isBefore(today) -> TaskStatus.EXPIRED
        else -> TaskStatus.PENDING
    }
}
// 👇 Pega esto al final de tu archivo SchoolTask.kt
/**
 * Datos capturados en CreateTaskScreen. `dateText` se convierte a la fecha ISO `YYYY-MM-DD`
 * que espera el contrato HTTP cuando la capa de red construye la petición.
 */
data class NewTaskInput(
    val title: String,
    val subject: Subject,
    val description: String,
    val dateText: String
)