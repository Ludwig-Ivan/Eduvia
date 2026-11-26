package com.example.librerias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppResult
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Registro específico de maestro: valida contraseña y términos y usa `RegisterUseCase`.
 * Expone `isLoading` y `error` a la pantalla de registro.
 */
class RegisterTeacherViewModel(private val container: AppContainer) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun registerTeacher(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        groups: List<TeacherGroup>,
        acceptedTerms: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (password != confirmPassword) {
                _error.value = "Las contraseñas no coinciden"
                _isLoading.value = false
                return@launch
            }

            if (!acceptedTerms) {
                _error.value = "Debes aceptar los términos y condiciones"
                _isLoading.value = false
                return@launch
            }

            val input = com.example.librerias.model.RegisterInput(
                role = UserRole.TEACHER,
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                confirmPassword = confirmPassword,
                acceptedTerms = acceptedTerms,
                children = emptyList(),
                groups = groups.map { com.example.librerias.model.GroupInput(it.groupName, it.schoolName, it.subject) }
            )

            when (val result = container.register(input)) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }
}