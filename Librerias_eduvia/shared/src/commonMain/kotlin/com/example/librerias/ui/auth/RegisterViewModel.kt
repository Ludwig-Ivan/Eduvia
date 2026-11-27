package com.example.librerias.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.librerias.di.AppContainer
import com.example.librerias.model.AppResult
import com.example.librerias.model.ChildInfo
import com.example.librerias.model.GroupInput
import com.example.librerias.model.RegisterInput
import com.example.librerias.model.RegistrationResult
import com.example.librerias.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Gestiona el alta de cuentas con `ValidateAccountStepUseCase` y `RegisterUseCase`: valida el
 * primer paso y registra padre o maestro. Expone `isLoading` y `error` a la UI.
 */
class RegisterViewModel(private val container: AppContainer) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() { _error.value = null }

    /** Fallo inesperado (red caída, excepción no mapeada): mensaje seguro y en español. */
    private fun showFailure(error: Exception) {
        _error.value = container.errorMessage(error)
    }

    /** Paso 1: formato + correo y celular que no existan ya. */
    fun validateStep1(
        fullName: String, email: String, phone: String,
        password: String, confirmPassword: String,
        onValid: () -> Unit
    ) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                when (val r = container.validateAccountStep(fullName, email, phone, password, confirmPassword)) {
                    is AppResult.Success -> onValid()
                    is AppResult.Error -> _error.value = r.message
                }
            } catch (e: Exception) {
                showFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerParent(
        fullName: String, email: String, phone: String, password: String, confirmPassword: String,
        children: List<ChildInfo>, acceptedTerms: Boolean, onSuccess: () -> Unit
    ) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val input = RegisterInput(
                role = UserRole.PARENT, fullName = fullName, email = email, phone = phone,
                password = password, confirmPassword = confirmPassword, acceptedTerms = acceptedTerms,
                children = children, groups = emptyList()
            )
            try {
                when (val result = container.register(input)) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Error -> _error.value = result.message
                }
            } catch (e: Exception) {
                showFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerTeacher(
        fullName: String, email: String, phone: String, password: String, confirmPassword: String,
        groups: List<TeacherGroup>, acceptedTerms: Boolean, onSuccess: (RegistrationResult) -> Unit
    ) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val input = RegisterInput(
                role = UserRole.TEACHER, fullName = fullName, email = email, phone = phone,
                password = password, confirmPassword = confirmPassword, acceptedTerms = acceptedTerms,
                children = emptyList(),
                groups = groups.map { GroupInput(it.groupName, it.schoolName, it.subject) }
            )
            try {
                when (val result = container.register(input)) {
                    is AppResult.Success -> onSuccess(result.value)
                    is AppResult.Error -> _error.value = result.message
                }
            } catch (e: Exception) {
                showFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}