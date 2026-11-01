package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.model.SettingsDto
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.data.repository.SettingsRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UiState<SettingsDto> = UiState.Loading,
    val theme: Theme = Theme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    preferencesStore: PreferencesStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { preferencesStore.theme.collect { theme -> _state.update { it.copy(theme = theme) } } }
        viewModelScope.launch { preferencesStore.notificationsEnabled.collect { enabled -> _state.update { it.copy(notificationsEnabled = enabled) } } }
        load()
    }

    fun load() {
        _state.update { it.copy(settings = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getSettings()) {
                is ApiResult.Success -> _state.update { it.copy(settings = UiState.Success(result.data, false), theme = result.data.theme, notificationsEnabled = result.data.notificationEnabled) }
                is ApiResult.Failure -> _state.update { it.copy(settings = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    fun setTheme(theme: Theme) {
        _state.update { it.copy(theme = theme, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.setTheme(theme)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, settings = UiState.Success(result.data, false)) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, fcmToken: String? = null) {
        _state.update { it.copy(notificationsEnabled = enabled, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.setNotificationsEnabled(enabled, fcmToken)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, settings = UiState.Success(result.data, false)) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun registerFcmToken(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.registerFcmToken(token)) {
                is ApiResult.Success -> _state.update { it.copy(settings = UiState.Success(result.data, false)) }
                is ApiResult.Failure -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
