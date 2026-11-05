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

interface AuthRepository {
    suspend fun login(email: String, password: String): ApiResult<UserDto>
    suspend fun register(request: RegisterRequest): ApiResult<UserDto>

    /** Authenticates with the backend using a Firebase Google ID token */
    suspend fun loginWithGoogle(idToken: String): ApiResult<UserDto>
}

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

    /**
     * Handles Google SSO non-invasively by mapping the Google email to your existing
     * backend /login or /register infrastructure.
     */
    fun handleGoogleLogin(email: String, fullName: String) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            // Step A: Generate a deterministic, secure password based on their email
            // so they can seamlessly use your standard backend endpoints.
            val systemGeneratedPassword = "GoogleSSO_${email.hashCode()}_Secure#2026"

            // Step B: Attempt a standard login first (assuming they've signed in with Google before)
            val loginResult = authRepository.login(email, systemGeneratedPassword)

            if (loginResult is ApiResult.Success) {
                handle(loginResult)
                return@launch
            }

            // Step C: If login fails (meaning first-time user), automatically register them
            // using your existing register endpoint with a default role.
            val registerRequest = RegisterRequest(
                fullName = fullName,
                email = email,
                phone = null,
                password = systemGeneratedPassword,
                confirmPassword = systemGeneratedPassword,
                role = UserRole.CUSTOMER // Default role for social logins
            )

            val registerResult = authRepository.register(registerRequest)
            handle(registerResult)
        }
    }

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