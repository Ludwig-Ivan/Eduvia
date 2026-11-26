package com.example.librerias.data.repository

import com.example.librerias.data.SchoolSampleData
import com.example.librerias.domain.repository.GroupRepository
import com.example.librerias.model.AppResult
import com.example.librerias.model.ChildInfo
import com.example.librerias.model.GroupInput
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Implementación en memoria de grupos y alumnos anterior a la API, conservada como reserva y ejemplo
 * (HALLAZGOS H10, OPEN-QUESTIONS Q12); la ruta activa es `data/remote`, inyectada por `AppContainer`.
 */
class GroupRepositoryImpl : GroupRepository {

    private val groups = MutableStateFlow(SchoolSampleData.groups)
    private val students = MutableStateFlow(SchoolSampleData.students)
    private var nextGroupId = (SchoolSampleData.groups.maxOfOrNull { it.id } ?: 0L) + 1
    private var nextStudentId = (SchoolSampleData.students.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeGroupsOf(teacherId: Long): Flow<List<SchoolGroup>> =
        groups.map { list -> list.filter { it.teacherId == teacherId } }

    override fun observeStudentsOf(parentId: Long): Flow<List<Student>> =
        students.map { list -> list.filter { it.parentId == parentId } }

    override fun observeStudentsOfGroup(groupId: Long): Flow<List<Student>> =
        students.map { list -> list.filter { it.groupId == groupId } }

    override suspend fun createGroup(teacherId: Long, input: GroupInput): AppResult<SchoolGroup> {
        val group = SchoolGroup(
            id = nextGroupId++,
            name = input.groupName.trim(),
            school = input.schoolName.trim(),
            subject = input.subject.trim(),
            code = generateUniqueCode(input.groupName),
            teacherId = teacherId
        )
        groups.update { it + group }
        return AppResult.Success(group)
    }

    override suspend fun findById(id: Long): SchoolGroup? =
        groups.value.firstOrNull { it.id == id }

    override suspend fun findByCode(code: String): SchoolGroup? =
        groups.value.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

    override suspend fun linkStudent(parentId: Long, child: ChildInfo): AppResult<Student> {
        val group = findByCode(child.classCode)
            ?: return AppResult.Error("El código ${child.classCode.trim()} no existe.")

        // Validamos que el alumno tenga nombre
        if (child.studentName.isBlank()) {
            return AppResult.Error("El nombre del alumno no puede estar vacío.")
        }

        val student = Student(
            id = nextStudentId++,
            name = child.studentName.trim(),
            groupId = group.id,
            parentId = parentId
        )
        students.update { it + student }

        // Actualizamos el contador de alumnos del grupo
        groups.update { list ->
            list.map { if (it.id == group.id) it.copy(studentCount = it.studentCount + 1) else it }
        }
        return AppResult.Success(student)
    }

    private fun generateUniqueCode(groupName: String): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val prefix = groupName.filter { it.isLetterOrDigit() }.uppercase().take(2).ifEmpty { "GR" }
        while (true) {
            val suffix = (1..4).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
            val code = "$prefix-$suffix"
            if (groups.value.none { it.code == code }) return code
        }
    }
}