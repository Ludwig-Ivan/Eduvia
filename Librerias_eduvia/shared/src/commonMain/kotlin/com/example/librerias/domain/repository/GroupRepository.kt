package com.example.librerias.domain.repository

import com.example.librerias.model.AppResult
import com.example.librerias.model.ChildInfo
import com.example.librerias.model.GroupInput
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.Student
import kotlinx.coroutines.flow.Flow

/**
 * Capa de dominio: grupos escolares, alumnos y vinculación de hijos con su padre.
 *
 * Lo satisfacen `GroupRepositoryImpl` (datos de muestra en memoria) y `RemoteGroupRepository`
 * (HTTP: `groups`, `groups/{id}`, `groups/by-code`, `groups/{groupId}/students`, `students` y
 * `students:link`). Los DTO del contrato de API se traducen a modelos del dominio, de modo que
 * la UI y los casos de uso no conocen la API.
 */
interface GroupRepository {
    /**
     * Observa los grupos impartidos por [teacherId].
     * @return flujo con los grupos de ese maestro.
     */
    fun observeGroupsOf(teacherId: Long): Flow<List<SchoolGroup>>

    /**
     * Observa los hijos vinculados a [parentId].
     * `parentId = 0` es el centinela de los alumnos sin padre asignado en los datos de muestra;
     * con un id real se obtienen únicamente los hijos de ese padre.
     * @return flujo con los alumnos del padre (o los alumnos sin padre si es 0).
     */
    fun observeStudentsOf(parentId: Long): Flow<List<Student>>

    /**
     * Observa los alumnos inscritos en [groupId].
     * @return flujo con los estudiantes del grupo.
     */
    fun observeStudentsOfGroup(groupId: Long): Flow<List<Student>>

    /**
     * Crea un grupo para [teacherId] a partir de [input] (nombre, escuela y materia).
     * @return el grupo creado con su código de clase, o `AppResult.Error` si falla la API.
     */
    suspend fun createGroup(teacherId: Long, input: GroupInput): AppResult<SchoolGroup>

    /**
     * Busca un grupo por [id].
     * @return el grupo, o null si no existe.
     */
    suspend fun findById(id: Long): SchoolGroup?

    /**
     * Busca un grupo por su código de clase [code], recortado y sin distinguir mayúsculas.
     * @return el grupo, o null si el código no existe (base de los códigos del registro).
     */
    suspend fun findByCode(code: String): SchoolGroup?

    /**
     * Vincula a [parentId] el hijo descrito en [child], resolviendo su grupo por código de clase.
     * @return el alumno creado, o `AppResult.Error` si el código no existe o falta el nombre.
     */
    suspend fun linkStudent(parentId: Long, child: ChildInfo): AppResult<Student>
}
