package com.example.librerias.api.service

import com.example.librerias.api.domain.SchoolGroup
import com.example.librerias.api.domain.Student
import com.example.librerias.api.repository.SchoolGroupRepository
import com.example.librerias.api.repository.StudentRepository
import com.example.librerias.api.web.badRequest
import com.example.librerias.api.web.conflict
import com.example.librerias.api.web.notFound
import com.example.librerias.api.web.dto.FieldErrorDto
import com.example.librerias.api.web.dto.LinkStudentRequest
import com.example.librerias.api.web.dto.PageResponse
import com.example.librerias.api.web.dto.toDto
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

/**
 * Servicio de grupos escolares y de los alumnos vinculados a una familia.
 *
 * Responsabilidad: listar y buscar grupos por id o por código compartible, crear grupos con un
 * código legible `XX-YYYY` único, listar alumnos por grupo o por tutor y vincular alumnos nuevos
 * actualizando el contador del grupo que muestra la app.
 *
 * Colaboradores: SchoolGroupRepository y StudentRepository. En la base de datos `parent_id` es
 * nullable mientras la app KMP representa «sin tutor» como `parentId = 0`; el mapper aplica la
 * conversión, de forma que los listados sigan siendo compatibles sin cambiar el esquema.
 */
@Service
class GroupService(
    private val groups: SchoolGroupRepository,
    private val students: StudentRepository,
) {
    private val random = SecureRandom()

    /**
     * Lista los grupos de un docente, paginados. Cada código se usa después desde la app para que
     * las familias lo introduzcan y vinculen a sus hijos.
     *
     * @param teacherId identificador del docente dueño de los grupos.
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `SchoolGroupDto`.
     */
    fun listByTeacher(teacherId: Long, pageable: Pageable) =
        PageResponse.of(groups.findAllByTeacherId(teacherId, pageable).map { it.toDto() })

    /**
     * Obtiene el detalle de un grupo por su identificador interno.
     *
     * @param id identificador del grupo.
     * @return `SchoolGroupDto` con nombre, colegio, asignatura, código y contador de alumnos.
     * @throws ApiException 404 si el grupo no existe, para que la app muestre «no encontrado» en
     * lugar de un grupo vacío.
     */
    fun get(id: Long) = groups.findById(id).orElseThrow { notFound("Grupo no encontrado") }.toDto()

    /**
     * Busca un grupo por el código que el docente reparte a las familias. La comparación ignora
     * mayúsculas y espacios porque el código se comunica por escrito u oralmente.
     *
     * @param code código de clase introducido en la app.
     * @return `SchoolGroupDto` del grupo encontrado.
     * @throws ApiException 404 si no hay ningún grupo con ese código; la app lo traduce al mensaje
     * de «código de clase inexistente».
     */
    fun getByCode(code: String) =
        groups.findByCodeIgnoreCase(code.trim())?.toDto() ?: notFound("Grupo no encontrado")

    /**
     * Crea un grupo para un docente y le asigna un código único generado en el servidor: esa
     * unicidad garantiza que el código compartido apunte siempre al mismo grupo. Nombre y colegio
     * son obligatorios porque la app los muestra en la pantalla de grupos.
     *
     * @param teacherId docente propietario del grupo.
     * @param groupName nombre visible del grupo.
     * @param schoolName colegio del grupo.
     * @param subject asignatura del grupo.
     * @return SchoolGroup persistido, con su código ya asignado.
     * @throws ApiException 400 si el nombre o el colegio están vacíos, con el error asociado al
     * campo para que la app pueda resaltarlo.
     * @throws ApiException 409 si tras varios intentos no se consigue un código único; dos grupos
     * con el mismo código harían ambigua la vinculación de alumnos.
     */
    @Transactional
    fun create(teacherId: Long, groupName: String, schoolName: String, subject: String): SchoolGroup {
        if (groupName.isBlank() || schoolName.isBlank()) {
            throw badRequest("Nombre y colegio son obligatorios")
        }
        return groups.save(
            SchoolGroup(
                name = groupName.trim(),
                school = schoolName.trim(),
                subject = subject.trim(),
                code = uniqueCode(),
                teacherId = teacherId,
            ),
        )
    }

    /**
     * Lista los alumnos de un grupo para la vista del docente. Los alumnos sin tutor llegan a la
     * app como `parentId = 0` aunque en la base de datos sean `NULL`.
     *
     * @param groupId grupo cuyos alumnos se listan.
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `StudentDto`.
     * @throws ApiException 404 si el grupo no existe; así no se confunde «sin tareas/alumnos» con
     * un grupo que no existe.
     */
    fun studentsOfGroup(groupId: Long, pageable: Pageable): PageResponse<com.example.librerias.api.web.dto.StudentDto> {
        if (!groups.existsById(groupId)) throw notFound("Grupo no encontrado")
        return PageResponse.of(students.findAllByGroupId(groupId, pageable).map { it.toDto() })
    }

    /**
     * Lista los hijos asociados a un tutor. La app usa `0` para «sin tutor», valor que el mapper
     * interpreta como `NULL` en la consulta; el mismo DTO sirve así para alumnos con y sin familia
     * asignada.
     *
     * @param parentId identificador del tutor (`0` cuando no hay tutor asignado).
     * @param pageable página y tamaño solicitados.
     * @return PageResponse de `StudentDto`.
     */
    fun studentsOfParent(parentId: Long, pageable: Pageable) =
        PageResponse.of(students.findAllByParentId(parentId, pageable).map { it.toDto() })

    /**
     * Vincula un alumno a un grupo a partir del código compartido y, en la misma transacción,
     * actualiza el contador de alumnos que la app muestra. El `parentId` de la base de datos admite
     * `NULL`, pero la app envía `0` cuando el alta no procede de un tutor concreto.
     *
     * @param req nombre del alumno, código de clase e identificador del tutor.
     * @return Student guardado, con el `groupId` resuelto a partir del código.
     * @throws ApiException 400 si el nombre está vacío, con error de campo, o 404 si el código de
     * clase no existe; sin grupo no se puede mantener el contador ni la agenda del alumno.
     */
    @Transactional
    fun linkStudent(req: LinkStudentRequest): Student {
        if (req.studentName.isBlank()) {
            throw badRequest("Nombre del alumno obligatorio", listOf(FieldErrorDto("studentName", "Requerido")))
        }
        val group = groups.findByCodeIgnoreCase(req.classCode.trim())
            ?: throw notFound("Código de clase inexistente")
        val student = students.save(
            Student(name = req.studentName.trim(), groupId = group.id, parentId = req.parentId),
        )
        group.studentCount++
        groups.save(group)
        return student
    }

    private fun uniqueCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(20) {
            val code = (1..2).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("") + "-" +
                (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
            if (!groups.existsByCodeIgnoreCase(code)) return code
        }
        throw conflict("No se pudo generar un código único")
    }
}
