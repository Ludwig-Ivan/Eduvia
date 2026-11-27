package com.example.librerias.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.Meeting
import com.example.librerias.model.SchoolTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Prepara el resumen de inicio: usa `ObserveMeetingsUseCase` y `ObserveTasksUseCase`.
 * Expone a la UI `meetings` (próximas juntas) y `tasks` (tareas del grupo activo).
 */
class HomeViewModel(private val container: AppContainer, private val groupId: Long) : ViewModel() {

    // Observa las juntas próximas
    val meetings: StateFlow<List<Meeting>> = container.observeMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observa las tareas del grupo activo, resuelto desde los grupos reales de la sesión
    val tasks: StateFlow<List<SchoolTask>> = container.observeTasks(groupId = groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}