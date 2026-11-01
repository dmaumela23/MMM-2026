package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.IncomingItemDto
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: UiState<List<OrderSummaryDto>> = UiState.Loading,
    val incoming: UiState<List<IncomingItemDto>> = UiState.Success(emptyList()),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Every account sees the orders it PLACED. Provider and Business accounts additionally see the
 * incoming order lines for their own products and services.
 */
class OrdersViewModel(
    authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    val user = authRepository.currentUser

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        val isSeller = Permissions.isSeller(user.value?.role)
        _state.value = OrdersUiState(
            orders = UiState.Loading,
            incoming = if (isSeller) UiState.Loading else UiState.Success(emptyList()),
        )
        loadJob = viewModelScope.launch {
            launch {
                when (val result = orderRepository.getOrders()) {
                    is ApiResult.Success -> _state.update {
                        it.copy(orders = UiState.Success(result.data.data, result.data.isStale))
                    }
                    is ApiResult.Failure -> _state.update {
                        it.copy(orders = UiState.Failure(result.error), errorMessage = result.error.message)
                    }
                }
            }
            if (isSeller) {
                launch {
                    when (val result = orderRepository.getIncomingItems()) {
                        is ApiResult.Success -> _state.update { it.copy(incoming = UiState.Success(result.data)) }
                        is ApiResult.Failure -> _state.update {
                            it.copy(incoming = UiState.Failure(result.error), errorMessage = result.error.message)
                        }
                    }
                }
            }
        }
    }

    fun refresh() = load()
    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
