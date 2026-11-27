package com.example.librerias.data.remote

import com.example.librerias.domain.repository.GroupRepository
import com.example.librerias.model.AppResult
import com.example.librerias.model.ChildInfo
import com.example.librerias.model.GroupInput
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.Student
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.Flow

/**
 * Implementación remota de GroupRepository: conserva la interfaz de dominio de la app.
 *
 * Implementa [GroupRepository] sobre HTTP (endpoints G1-G7), mapeando las respuestas `GroupDto` y
 * `StudentDto` a modelos de dominio con `toModel`. Mantiene tres cachés [RemoteListStore] separadas
 * por `teacherId`, `parentId` y `groupId` para que los flujos de la UI reaccionen a los cambios
 * locales sin esperar otra petición. Las mutaciones devuelven `AppResult.Error` con el mensaje de
 * la API; las búsquedas por id/código devuelven `null` ante un `ApiException`.
 */
class RemoteGroupRepository(
    private val api: ApiClient,
    reporter: RemoteErrorReporter? = null,
) : GroupRepository {

    private val groups = RemoteListStore<Long, SchoolGroup>(reporter)
    private val parentStudents = RemoteListStore<Long, Student>(reporter)
    private val groupStudents = RemoteListStore<Long, Student>(reporter)

    /**
     * `GET groups?teacherId=` (G1): lee `PageDto<GroupDto>`, mapea cada elemento a [SchoolGroup] y lo
     * cachea por `teacherId`. La primera suscripción descarga; un fallo de red no emite y conserva
     * el último valor cacheado.
     */
    override fun observeGroupsOf(teacherId: Long): Flow<List<SchoolGroup>> =
        groups.flow(teacherId) {
            api.get<PageDto<GroupDto>>("groups?teacherId=$teacherId&page=0&size=100")
                .items.map { it.toModel() }
        }

    /**
     * `GET students?parentId=` (G6): mapea `PageDto<StudentDto>` a [Student] y cachea el resultado por
     * `parentId` (`parentId = 0` representa «sin tutor»). Los errores de carga se ignoran en la caché.
     */
    override fun observeStudentsOf(parentId: Long): Flow<List<Student>> =
        parentStudents.flow(parentId) {
            api.get<PageDto<StudentDto>>("students?parentId=$parentId&page=0&size=100")
                .items.map { it.toModel() }
        }

    /**
     * `GET groups/{id}/students` (G5): mapea `PageDto<StudentDto>` a [Student] y cachea la lista por
     * `groupId` para alimentar los flujos de la UI; los fallos de red no sustituyen la caché.
     */
    override fun observeStudentsOfGroup(groupId: Long): Flow<List<Student>> =
        groupStudents.flow(groupId) {
            api.get<PageDto<StudentDto>>("groups/$groupId/students?page=0&size=100")
                .items.map { it.toModel() }
        }

    /**
     * `POST groups` (G4): envía [CreateGroupRequestDto] con los campos recortados, mapea el
     * `GroupDto` devuelto a [SchoolGroup] y lo añade a la caché de ese `teacherId` si no estaba.
     * Errores → `AppResult.Error`.
     */
    override suspend fun createGroup(teacherId: Long, input: GroupInput): AppResult<SchoolGroup> = try {
        val dto: GroupDto = api.post(
            "groups",
            CreateGroupRequestDto(
                teacherId = teacherId,
                groupName = input.groupName.trim(),
                schoolName = input.schoolName.trim(),
                subject = input.subject.trim(),
            ),
        )
        val group = dto.toModel()
        groups.update(teacherId) { current -> if (current.any { it.id == group.id }) current else current + group }
        AppResult.Success(group)
    } catch (e: Exception) {
        api.failure(e)
    }

    /**
     * `GET groups/{id}` (G2): mapea el `GroupDto` a [SchoolGroup] sin tocar las cachés; un 404 u
     * otro error de API se traduce a `null`.
     */
    override suspend fun findById(id: Long): AppResult<SchoolGroup?> = try {
        AppResult.Success(api.get<GroupDto>("groups/$id").toModel())
    } catch (e: ApiException) {
        if (e.status == 404) AppResult.Success(null) else api.failure(e)
    } catch (e: Exception) {
        api.failure(e)
    }

    /**
     * `GET groups/by-code?code=` (G3): recorta y codifica el código como parámetro de query, mapea
     * el `GroupDto` a [SchoolGroup] y devuelve `null` si la API responde con error (por ejemplo 404).
     */
    override suspend fun findByCode(code: String): AppResult<SchoolGroup?> = try {
        AppResult.Success(
            api.get<GroupDto>("groups/by-code?code=${code.trim().encodeURLParameter()}").toModel()
        )
    } catch (e: ApiException) {
        if (e.status == 404) AppResult.Success(null) else api.failure(e)
    } catch (e: Exception) {
        api.failure(e)
    }

    /**
     * `POST students:link` (G7): envía [LinkStudentRequestDto], mapea el [Student] devuelto y lo
     * añade a las cachés del tutor y del grupo; después recarga todos los listados de grupos para
     * reflejar el `studentCount` actualizado. Errores → `AppResult.Error`.
     */
    override suspend fun linkStudent(parentId: Long, child: ChildInfo): AppResult<Student> = try {
        val dto: StudentDto = api.post(
            "students:link",
            LinkStudentRequestDto(
                parentId = parentId,
                classCode = child.classCode.trim(),
                studentName = child.studentName.trim(),
            ),
        )
        val student = dto.toModel()
        parentStudents.update(parentId) { current ->
            if (current.any { it.id == student.id }) current else current + student
        }
        groupStudents.update(student.groupId) { current ->
            if (current.any { it.id == student.id }) current else current + student
        }
        // El backend incrementa studentCount; recargamos los listados de grupos observados.
        groups.refreshAll()
        AppResult.Success(student)
    } catch (e: Exception) {
        api.failure(e)
    }

    /** Vuelve a descargar todos los listados observados; lo usa el botón de reintento de la UI. */
    suspend fun refreshAll() {
        groups.refreshAll()
        parentStudents.refreshAll()
        groupStudents.refreshAll()
    }
}
