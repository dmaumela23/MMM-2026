package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val featuredProducts: UiState<List<ProductDto>> = UiState.Loading,
    val featuredServices: UiState<List<ServiceDto>> = UiState.Loading,
)

/** The dashboard: a welcome for the logged-in user and a short "featured" row of each kind. */
class HomeViewModel(
    authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    /** The logged-in user (null while logged out). */
    val user: StateFlow<UserDto?> = authRepository.currentUser

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { HomeUiState() }
        viewModelScope.launch {
            when (val result = productRepository.getProducts()) {
                is ApiResult.Success -> _state.update {
                    it.copy(featuredProducts = UiState.Success(result.data.data.take(FEATURED_COUNT), result.data.isStale))
                }
                is ApiResult.Failure -> _state.update { it.copy(featuredProducts = UiState.Failure(result.error)) }
            }
        }
        viewModelScope.launch {
            when (val result = serviceRepository.getServices()) {
                is ApiResult.Success -> _state.update {
                    it.copy(featuredServices = UiState.Success(result.data.data.take(FEATURED_COUNT), result.data.isStale))
                }
                is ApiResult.Failure -> _state.update { it.copy(featuredServices = UiState.Failure(result.error)) }
            }
        }
    }

    companion object {
        const val FEATURED_COUNT = 4
    }
}