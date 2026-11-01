package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyReportsResponse
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.data.repository.EnergyRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EnergyUiState(
    val selectedDays: Int = 30,
    val usage: UiState<EnergyUsageResponse> = UiState.Loading,
    val plans: UiState<EnergyPlansResponse> = UiState.Loading,
    val reports: UiState<EnergyReportsResponse> = UiState.Loading,
    val errorMessage: String? = null,
)

class EnergyViewModel(private val repository: EnergyRepository) : ViewModel() {
    private val _state = MutableStateFlow(EnergyUiState())
    val state: StateFlow<EnergyUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val days = _state.value.selectedDays
        _state.update { it.copy(usage = UiState.Loading, plans = UiState.Loading, reports = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getUsage(days)) {
                is ApiResult.Success -> _state.update { it.copy(usage = UiState.Success(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(usage = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
        viewModelScope.launch {
            when (val result = repository.getPlans()) {
                is ApiResult.Success -> _state.update { it.copy(plans = UiState.Success(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(plans = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
        viewModelScope.launch {
            when (val result = repository.getReports()) {
                is ApiResult.Success -> _state.update { it.copy(reports = UiState.Success(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(reports = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    fun setDays(days: Int) {
        if (days !in EnergyRepository.ALLOWED_DAYS) return
        if (_state.value.selectedDays == days) return
        _state.update { it.copy(selectedDays = days) }
        loadUsage()
    }

    private fun loadUsage() {
        val days = _state.value.selectedDays
        _state.update { it.copy(usage = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getUsage(days)) {
                is ApiResult.Success -> _state.update { it.copy(usage = UiState.Success(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(usage = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    fun refresh() = load()
    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
