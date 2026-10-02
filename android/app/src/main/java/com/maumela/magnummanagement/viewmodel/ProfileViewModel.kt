package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isSaving: Boolean = false,
    val errors: Validators.ProfileErrors = Validators.ProfileErrors(),
    val passwordErrors: Validators.PasswordChangeErrors = Validators.PasswordChangeErrors(),
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val passwordChanged: Boolean = false,
)

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val user: StateFlow<UserDto?> = authRepository.currentUser
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun saveProfile(fullName: String, email: String, phone: String) {
        if (_state.value.isSaving) return
        val errors = Validators.profile(fullName, email, phone)
        if (errors.hasErrors) { _state.update { it.copy(errors = errors, errorMessage = null, saved = false) }; return }
        val id = user.value?.id ?: run { _state.update { it.copy(errorMessage = "You are not logged in.") }; return }
        _state.update { it.copy(isSaving = true, errors = Validators.ProfileErrors(), errorMessage = null, saved = false) }
        viewModelScope.launch {
            when (val result = authRepository.updateProfile(id, fullName, email, phone.ifBlank { null })) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        if (_state.value.isSaving) return
        val errors = Validators.passwordChange(current, new, confirm)
        if (errors.hasErrors) { _state.update { it.copy(passwordErrors = errors, errorMessage = null, passwordChanged = false) }; return }
        val id = user.value?.id ?: run { _state.update { it.copy(errorMessage = "You are not logged in.") }; return }
        _state.update { it.copy(isSaving = true, passwordErrors = Validators.PasswordChangeErrors(), errorMessage = null, passwordChanged = false) }
        viewModelScope.launch {
            when (val result = authRepository.changePassword(id, current, new)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, passwordChanged = true) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, saved = false, passwordChanged = false) }
    fun clearValidationErrors() = _state.update { it.copy(errors = Validators.ProfileErrors(), passwordErrors = Validators.PasswordChangeErrors(), errorMessage = null) }
}
