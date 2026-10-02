package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.RegisterRequest
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val loginErrors: Validators.LoginErrors = Validators.LoginErrors(),
    val registerErrors: Validators.RegisterErrors = Validators.RegisterErrors(),
    /** A message from the server (wrong password, email already registered, no internet...). */
    val errorMessage: String? = null,
    /** Set when login or registration succeeded. The screen navigates, then calls onAuthenticationHandled(). */
    val authenticatedUser: UserDto? = null,
)

/** Login and registration. Invalid forms are blocked here and never reach the network. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (_state.value.isLoading) return // ignore a double tap
        val errors = Validators.login(email, password)
        if (errors.hasErrors) {
            _state.update { it.copy(loginErrors = errors, errorMessage = null) }
            return
        }
        _state.update { it.copy(isLoading = true, loginErrors = Validators.LoginErrors(), errorMessage = null) }
        viewModelScope.launch {
            handle(authRepository.login(email, password))
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
    ) {
        if (_state.value.isLoading) return
        val errors = Validators.register(fullName, email, phone, password, confirmPassword)
        if (errors.hasErrors) {
            _state.update { it.copy(registerErrors = errors, errorMessage = null) }
            return
        }
        _state.update {
            it.copy(isLoading = true, registerErrors = Validators.RegisterErrors(), errorMessage = null)
        }
        val request = RegisterRequest(
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim().ifEmpty { null },
            password = password,
            confirmPassword = confirmPassword,
            role = role,
        )
        viewModelScope.launch {
            handle(authRepository.register(request))
        }
    }

    private fun handle(result: ApiResult<UserDto>) {
        when (result) {
            is ApiResult.Success ->
                _state.update { it.copy(isLoading = false, authenticatedUser = result.data) }
            is ApiResult.Failure ->
                _state.update { it.copy(isLoading = false, errorMessage = result.error.message) }
        }
    }

    /** Called when the user edits a field, so an old error message does not linger. */
    fun clearErrors() {
        _state.update {
            it.copy(
                loginErrors = Validators.LoginErrors(),
                registerErrors = Validators.RegisterErrors(),
                errorMessage = null,
            )
        }
    }

    fun onAuthenticationHandled() {
        _state.update { it.copy(authenticatedUser = null) }
    }
}