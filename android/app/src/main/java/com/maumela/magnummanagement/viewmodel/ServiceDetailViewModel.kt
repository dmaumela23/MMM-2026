package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.data.model.ServiceUpdateRequest
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServiceDetailUiState(
    val service: UiState<ServiceDto> = UiState.Loading,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class ServiceDetailViewModel(
    private val repository: ServiceRepository,
    private val serviceId: Int?,
) : ViewModel() {
    private val _state = MutableStateFlow(ServiceDetailUiState())
    val state: StateFlow<ServiceDetailUiState> = _state.asStateFlow()

    init { if (serviceId == null) _state.value = ServiceDetailUiState(service = UiState.Failure(com.maumela.magnummanagement.data.api.AppError(com.maumela.magnummanagement.data.api.ErrorKind.BAD_REQUEST, "No service selected."))) else load() }

    fun load() {
        val id = serviceId ?: return
        _state.update { it.copy(service = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getService(id)) {
                is ApiResult.Success -> _state.update { it.copy(service = UiState.Success(result.data.data, result.data.isStale)) }
                is ApiResult.Failure -> _state.update { it.copy(service = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    fun create(request: ServiceRequest) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null, saved = false) }
        viewModelScope.launch { when (val result = repository.createService(request)) {
            is ApiResult.Success -> _state.update { it.copy(service = UiState.Success(result.data, false), isWorking = false, saved = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun update(request: ServiceUpdateRequest) {
        val id = serviceId ?: return
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null, saved = false) }
        viewModelScope.launch { when (val result = repository.updateService(id, request)) {
            is ApiResult.Success -> _state.update { it.copy(service = UiState.Success(result.data, false), isWorking = false, saved = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun delete() {
        val id = serviceId ?: return
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch { when (val result = repository.deleteService(id)) {
            is ApiResult.Success -> _state.update { it.copy(isWorking = false, deleted = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun clearFlags() = _state.update { it.copy(saved = false, deleted = false, errorMessage = null) }

    companion object {
        fun validate(name: String, category: String, price: String, deliveryDays: String): String? =
            Validators.listingName(name) ?: Validators.listingCategory(category) ?: Validators.price(price) ?: Validators.deliveryDays(deliveryDays)
    }
}
