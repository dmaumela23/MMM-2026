package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.ProductUpdateRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.Validators
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: UiState<ProductDto> = UiState.Loading,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class ProductDetailViewModel(
    private val repository: ProductRepository,
    private val productId: Int?,
) : ViewModel() {
    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    init { if (productId == null) _state.value = ProductDetailUiState(product = UiState.Failure(com.maumela.magnummanagement.data.api.AppError(com.maumela.magnummanagement.data.api.ErrorKind.BAD_REQUEST, "No product selected."))) else load() }

    fun load() {
        val id = productId ?: return
        _state.update { it.copy(product = UiState.Loading, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getProduct(id)) {
                is ApiResult.Success -> _state.update { it.copy(product = UiState.Success(result.data.data, result.data.isStale)) }
                is ApiResult.Failure -> _state.update { it.copy(product = UiState.Failure(result.error), errorMessage = result.error.message) }
            }
        }
    }

    fun create(request: ProductRequest) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null, saved = false) }
        viewModelScope.launch { when (val result = repository.createProduct(request)) {
            is ApiResult.Success -> _state.update { it.copy(product = UiState.Success(result.data, false), isWorking = false, saved = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun update(request: ProductUpdateRequest) {
        val id = productId ?: return
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null, saved = false) }
        viewModelScope.launch { when (val result = repository.updateProduct(id, request)) {
            is ApiResult.Success -> _state.update { it.copy(product = UiState.Success(result.data, false), isWorking = false, saved = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun delete() {
        val id = productId ?: return
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch { when (val result = repository.deleteProduct(id)) {
            is ApiResult.Success -> _state.update { it.copy(isWorking = false, deleted = true) }
            is ApiResult.Failure -> _state.update { it.copy(isWorking = false, errorMessage = result.error.message) }
        } }
    }

    fun clearFlags() = _state.update { it.copy(saved = false, deleted = false, errorMessage = null) }

    companion object {
        /** Shared validation helper for the reusable ListingEditorSheet. */
        fun validate(name: String, category: String, price: String, stock: String, imageUrl: String): String? =
            Validators.listingName(name) ?: Validators.listingCategory(category) ?: Validators.price(price) ?: Validators.stockQuantity(stock) ?: Validators.imageUrl(imageUrl)
    }
}
