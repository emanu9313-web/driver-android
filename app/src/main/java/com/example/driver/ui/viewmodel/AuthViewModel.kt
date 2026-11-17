package com.example.driver.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver.domain.model.User
import com.example.driver.domain.model.UserRole
import com.example.driver.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState(error = "Ingresa correo y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.login(email.trim(), pass)
            result.onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { err ->
                _uiState.value = AuthUiState(error = err.localizedMessage ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(
        email: String,
        pass: String,
        name: String,
        phone: String,
        role: UserRole,
        vehicleModel: String = "",
        vehiclePlate: String = "",
        vehicleColor: String = ""
    ) {
        if (email.isBlank() || pass.isBlank() || name.isBlank() || phone.isBlank()) {
            _uiState.value = AuthUiState(error = "Completa todos los campos obligatorios")
            return
        }
        if (role == UserRole.CONDUCTOR && (vehicleModel.isBlank() || vehiclePlate.isBlank())) {
            _uiState.value = AuthUiState(error = "Completa la información del vehículo")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.register(
                email = email.trim(),
                pass = pass,
                name = name.trim(),
                phone = phone.trim(),
                role = role,
                vehicleModel = vehicleModel.trim(),
                vehiclePlate = vehiclePlate.trim().uppercase(),
                vehicleColor = vehicleColor.trim()
            )
            result.onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { err ->
                _uiState.value = AuthUiState(error = err.localizedMessage ?: "Error al registrarse")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }
}
