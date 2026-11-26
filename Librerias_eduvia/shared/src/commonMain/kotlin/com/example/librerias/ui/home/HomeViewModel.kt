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
 * Expone a la UI `meetings` (próximas juntas) y `tasks` (tareas del grupo de la demo).
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    // Observa las juntas próximas
    val meetings: StateFlow<List<Meeting>> = container.observeMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observa las tareas (asumimos que el grupo 1 es el del usuario actual para esta demo)
    val tasks: StateFlow<List<SchoolTask>> = container.observeTasks(groupId = 1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}