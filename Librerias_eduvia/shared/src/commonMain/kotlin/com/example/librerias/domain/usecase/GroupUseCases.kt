package com.example.librerias.domain.usecase

import com.example.librerias.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import com.example.librerias.model.SchoolGroup

/**
 * Lista los grupos de un maestro. En la API se consulta `groups?teacherId=...` y la respuesta
 * se cachea por maestro, de modo que la lista se actualiza sola tras crear un grupo.
 *
 * @param groups repositorio de grupos y alumnos.
 * @return flujo con los grupos del maestro indicado.
 */
class ObserveGroupsUseCase(private val groups: GroupRepository) {
    /**
     * @param teacherId id del maestro propietario de los grupos.
     * @return flujo de grupos de ese maestro.
     */
    operator fun invoke(teacherId: Long): Flow<List<SchoolGroup>> =
        groups.observeGroupsOf(teacherId)
}

/**
 * Lista los hijos vinculados a un padre. `parentId = 0` es el centinela de los alumnos sin
 * padre en los datos de muestra; con un id real solo se ven los hijos de esa cuenta.
 *
 * @param groups repositorio de grupos y alumnos.
 * @return flujo con los alumnos del padre (o los alumnos sin padre si el id es 0).
 */
class ObserveStudentsUseCase(private val groups: GroupRepository) {
    /**
     * @param parentId id del padre, o 0 para los alumnos sin padre asignado.
     * @return flujo de alumnos correspondientes a ese padre.
     */
    operator fun invoke(parentId: Long) = groups.observeStudentsOf(parentId)
}
