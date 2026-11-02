package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ItemType
import com.maumela.magnummanagement.data.model.OrderItemRequest
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.PriceCalculator
import com.maumela.magnummanagement.utils.Validators
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One line of the order being built. Prices shown here are a PREVIEW; the server sets the real ones. */
data class DraftItem(
    val itemType: ItemType,
    val id: Int,
    val name: String,
    val sellerName: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    /** Products: stock or 99, whichever is lower. Services: always 1. */
    val maxQuantity: Int,
) {
    val key: String get() = keyOf(itemType, id)
    val lineTotal: BigDecimal get() = PriceCalculator.lineTotal(unitPrice, quantity)

    companion object {
        fun keyOf(type: ItemType, id: Int) = "${type.name}:$id"
    }
}

data class DraftUiState(
    val items: List<DraftItem> = emptyList(),
    val notes: String = "",
    val notesError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Set after a successful submit. The screen navigates to that order, then calls onOrderCreatedHandled(). */
    val createdOrderId: Int? = null,
) {
    val total: BigDecimal get() = PriceCalculator.orderTotal(items.map { it.unitPrice to it.quantity })
    val itemCount: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
}

enum class AddResult { ADDED, QUANTITY_INCREASED, ALREADY_IN_ORDER, LIMIT_REACHED, UNAVAILABLE }

/**
 * The order being built before checkout. Shared by the whole app (activity-scoped), so the
 * Marketplace, the detail screens and the Orders screen all see the same draft.
 * It lives in memory only: it is lost if Android kills the app, which is fine for a prototype.
 */
class OrderDraftViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _state = MutableStateFlow(DraftUiState())
    val state: StateFlow<DraftUiState> = _state.asStateFlow()

    fun addProduct(product: ProductDto, quantity: Int = 1): AddResult {
        val availability = DisplayRules.productAvailability(product.availability, product.stockQuantity)
        if (!availability.canOrder) return AddResult.UNAVAILABLE

        val max = minOf(PriceCalculator.MAX_PRODUCT_QUANTITY, product.stockQuantity)
        val key = DraftItem.keyOf(ItemType.PRODUCT, product.id)
        val existing = _state.value.items.firstOrNull { it.key == key }

        if (existing == null) {
            val item = DraftItem(
                itemType = ItemType.PRODUCT,
                id = product.id,
                name = product.name,
                sellerName = product.sellerName,
                unitPrice = product.price,
                quantity = quantity.coerceIn(1, max),
                maxQuantity = max,
            )
            _state.update { it.copy(items = it.items + item, errorMessage = null) }
            return AddResult.ADDED
        }
        if (existing.quantity >= max) return AddResult.LIMIT_REACHED
        setQuantity(key, existing.quantity + quantity)
        return AddResult.QUANTITY_INCREASED
    }

    /** Service quantity is always 1, so a service can only be added once. */
    fun addService(service: ServiceDto): AddResult {
        if (!DisplayRules.serviceAvailability(service.availability).canOrder) return AddResult.UNAVAILABLE
        val key = DraftItem.keyOf(ItemType.SERVICE, service.id)
        if (_state.value.items.any { it.key == key }) return AddResult.ALREADY_IN_ORDER
        val item = DraftItem(
            itemType = ItemType.SERVICE,
            id = service.id,
            name = service.name,
            sellerName = service.providerName,
            unitPrice = service.price,
            quantity = 1,
            maxQuantity = 1,
        )
        _state.update { it.copy(items = it.items + item, errorMessage = null) }
        return AddResult.ADDED
    }

    fun setQuantity(key: String, quantity: Int) {
        _state.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.key == key) item.copy(quantity = quantity.coerceIn(1, item.maxQuantity)) else item
                },
            )
        }
    }

    fun remove(key: String) {
        _state.update { it.copy(items = it.items.filterNot { item -> item.key == key }) }
    }

    fun setNotes(text: String) {
        _state.update { it.copy(notes = text, notesError = null) }
    }

    /** Also called on logout, so the next account never sees the previous user's draft. */
    fun clear() {
        _state.value = DraftUiState()
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /** Sends ids and quantities only. The server loads current prices and calculates the total. */
    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (current.items.isEmpty()) {
            _state.update { it.copy(errorMessage = "Your order is empty. Add a product or service first.") }
            return
        }
        val notesError = Validators.orderNotes(current.notes)
        if (notesError != null) {
            _state.update { it.copy(notesError = notesError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, errorMessage = null, notesError = null) }
        val lines = current.items.map { item ->
            when (item.itemType) {
                ItemType.PRODUCT -> OrderItemRequest(productId = item.id, quantity = item.quantity)
                ItemType.SERVICE -> OrderItemRequest(serviceId = item.id, quantity = 1)
            }
        }
        viewModelScope.launch {
            when (val result = orderRepository.createOrder(lines, current.notes)) {
                is ApiResult.Success -> _state.value = DraftUiState(createdOrderId = result.data.id)
                is ApiResult.Failure ->
                    _state.update { it.copy(isSubmitting = false, errorMessage = result.error.message) }
            }
        }
    }

    fun onOrderCreatedHandled() {
        _state.update { it.copy(createdOrderId = null) }
    }
}