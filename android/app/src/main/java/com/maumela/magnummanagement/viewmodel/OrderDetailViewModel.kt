package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: UiState<OrderDto> = UiState.Loading,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
)

class OrderDetailViewModel(
    private val orderRepository: OrderRepository,
    private val orderId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailUiState())
    val state: StateFlow<OrderDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(order = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = orderRepository.getOrder(orderId)) {
                is ApiResult.Success -> _state.update { it.copy(order = UiState.Success(result.data, false)) }
                is ApiResult.Failure -> _state.update { it.copy(order = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    /** Customer cancellation is allowed by the server only while every line is pending. */
    fun cancelOrder() {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = orderRepository.cancelOrder(orderId)) {
                is ApiResult.Success -> _state.update { it.copy(order = UiState.Success(result.data, false), isWorking = false) }
                is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
            }
        }
    }

    /** Provider/business status update. The backend remains the authority for allowed transitions. */
    fun updateItemStatus(itemId: Int, status: ItemStatus) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = orderRepository.updateItemStatus(orderId, itemId, status)) {
                is ApiResult.Success -> {
                    when (val refreshed = orderRepository.getOrder(orderId)) {
                        is ApiResult.Success -> _state.update { it.copy(order = UiState.Success(refreshed.data, false), isWorking = false) }
                        is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = refreshed.error.message) }
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
