package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileOverviewState(
    /** null until the order list has loaded (or if it could not be loaded). */
    val orderCount: Int? = null,
    val products: UiState<List<ProductDto>> = UiState.Success(emptyList()),
    val services: UiState<List<ServiceDto>> = UiState.Success(emptyList()),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedMessage: String? = null,
)

/**
 * Profile page data: how many orders I have placed, and (Provider/Business only) my own
 * listings with the ability to create new ones. The server enforces who may create what.
 */
class ProfileOverviewViewModel(
    authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {
    val user: StateFlow<UserDto?> = authRepository.currentUser

    private val _state = MutableStateFlow(ProfileOverviewState())
    val state: StateFlow<ProfileOverviewState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val role = user.value?.role
        _state.update {
            it.copy(
                products = if (Permissions.canCreateProducts(role)) UiState.Loading else UiState.Success(emptyList()),
                services = if (Permissions.canCreateServices(role)) UiState.Loading else UiState.Success(emptyList()),
            )
        }
        viewModelScope.launch {
            val result = orderRepository.getOrders()
            if (result is ApiResult.Success) _state.update { it.copy(orderCount = result.data.data.size) }
        }
        if (Permissions.canCreateProducts(role)) loadProducts()
        if (Permissions.canCreateServices(role)) loadServices()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            when (val result = productRepository.getProducts(ListingFilter(ownerOnly = true))) {
                is ApiResult.Success -> _state.update { it.copy(products = UiState.Success(result.data.data)) }
                is ApiResult.Failure -> _state.update { it.copy(products = UiState.Failure(result.error)) }
            }
        }
    }

    private fun loadServices() {
        viewModelScope.launch {
            when (val result = serviceRepository.getServices(ListingFilter(ownerOnly = true))) {
                is ApiResult.Success -> _state.update { it.copy(services = UiState.Success(result.data.data)) }
                is ApiResult.Failure -> _state.update { it.copy(services = UiState.Failure(result.error)) }
            }
        }
    }

    fun createProduct(request: ProductRequest) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, errorMessage = null, savedMessage = null) }
        viewModelScope.launch {
            when (val result = productRepository.createProduct(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false, savedMessage = "Product added") }
                    loadProducts()
                }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun createService(request: ServiceRequest) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, errorMessage = null, savedMessage = null) }
        viewModelScope.launch {
            when (val result = serviceRepository.createService(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false, savedMessage = "Service added") }
                    loadServices()
                }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, errorMessage = result.error.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, savedMessage = null) }
}
