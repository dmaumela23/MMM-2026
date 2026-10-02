package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.Validators
import java.math.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MarketplaceTab {
    PRODUCTS, SERVICES;

    companion object {
        fun fromArg(arg: String?): MarketplaceTab =
            entries.firstOrNull { it.name.equals(arg, ignoreCase = true) } ?: PRODUCTS
    }
}

/** What the user has chosen to search and filter by. The SERVER applies it as SQL conditions. */
data class MarketplaceFilters(
    val query: String = "",
    val sector: Sector? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRating: Double? = null,
    val availableOnly: Boolean = false,
    // Categories differ between products and services, so each tab has its own.
    val productCategory: String? = null,
    val serviceCategory: String? = null,
) {
    val activeFilterCount: Int
        get() = listOf(
            sector != null, minPrice != null, maxPrice != null, minRating != null,
            availableOnly, productCategory != null, serviceCategory != null,
        ).count { it }

    internal fun toProductFilter() = toListingFilter(productCategory)
    internal fun toServiceFilter() = toListingFilter(serviceCategory)

    private fun toListingFilter(category: String?) = ListingFilter(
        query = query.trim().ifEmpty { null },
        category = category,
        sector = sector,
        minPrice = minPrice,
        maxPrice = maxPrice,
        minRating = minRating,
        availableOnly = availableOnly,
    )
}

data class MarketplaceUiState(
    val tab: MarketplaceTab = MarketplaceTab.PRODUCTS,
    val filters: MarketplaceFilters = MarketplaceFilters(),
    val products: UiState<List<ProductDto>> = UiState.Loading,
    val services: UiState<List<ServiceDto>> = UiState.Loading,
    /** Category chips, taken from the last UNFILTERED load so they never disappear while filtering. */
    val productCategories: List<String> = emptyList(),
    val serviceCategories: List<String> = emptyList(),
    val filterError: String? = null,
)

class MarketplaceViewModel(
    private val productRepository: ProductRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MarketplaceUiState())
    val state: StateFlow<MarketplaceUiState> = _state.asStateFlow()

    private var productsJob: Job? = null
    private var servicesJob: Job? = null
    private var started = false

    /** Load both lists the first time the screen opens. */
    fun ensureLoaded() {
        if (started) return
        started = true
        reload()
    }

    /** Opened from Home (a sector tile or the search bar) with a tab and/or search text. */
    fun openWith(tab: MarketplaceTab, query: String?) {
        val newQuery = query.orEmpty()
        val firstTime = !started
        started = true
        val current = _state.value
        if (!firstTime && current.tab == tab && current.filters.query == newQuery) return
        _state.update { it.copy(tab = tab, filters = it.filters.copy(query = newQuery)) }
        reload()
    }

    fun setTab(tab: MarketplaceTab) {
        _state.update { it.copy(tab = tab) }
    }

    /** Typing only edits the text. The search runs when the user submits it. */
    fun setQuery(text: String) {
        _state.update { it.copy(filters = it.filters.copy(query = text)) }
    }

    fun submitSearch() = reload()

    fun clearSearch() {
        _state.update { it.copy(filters = it.filters.copy(query = "")) }
        reload()
    }

    fun setProductCategory(category: String?) {
        _state.update { it.copy(filters = it.filters.copy(productCategory = category)) }
        loadProducts()
    }

    fun setServiceCategory(category: String?) {
        _state.update { it.copy(filters = it.filters.copy(serviceCategory = category)) }
        loadServices()
    }

    /**
     * Applies the filter sheet. Returns true when applied, or false after setting [MarketplaceUiState.filterError].
     * Prices are typed as text, so they are checked here.
     */
    fun applyFilters(
        sector: Sector?,
        minPriceText: String,
        maxPriceText: String,
        minRating: Double?,
        availableOnly: Boolean,
    ): Boolean {
        val minText = minPriceText.trim()
        val maxText = maxPriceText.trim()
        val min = if (minText.isEmpty()) null else Validators.parsePrice(minText, allowZero = true)
        val max = if (maxText.isEmpty()) null else Validators.parsePrice(maxText, allowZero = true)

        val error = when {
            minText.isNotEmpty() && min == null -> "Enter a valid minimum price"
            maxText.isNotEmpty() && max == null -> "Enter a valid maximum price"
            min != null && max != null && min > max -> "Minimum price cannot be higher than the maximum"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(filterError = error) }
            return false
        }
        _state.update {
            it.copy(
                filterError = null,
                filters = it.filters.copy(
                    sector = sector,
                    minPrice = min,
                    maxPrice = max,
                    minRating = minRating,
                    availableOnly = availableOnly,
                ),
            )
        }
        reload()
        return true
    }

    /** Removes every filter but keeps the search text. */
    fun clearFilters() {
        _state.update {
            it.copy(filterError = null, filters = MarketplaceFilters(query = it.filters.query))
        }
        reload()
    }

    fun clearFilterError() {
        _state.update { it.copy(filterError = null) }
    }

    /** Pull-to-refresh and the Try again button. */
    fun refresh() = reload()

    private fun reload() {
        loadProducts()
        loadServices()
    }

    private fun loadProducts() {
        productsJob?.cancel() // a newer request replaces an older, slower one
        val filter = _state.value.filters.toProductFilter()
        _state.update { it.copy(products = UiState.Loading) }
        productsJob = viewModelScope.launch {
            when (val result = productRepository.getProducts(filter)) {
                is ApiResult.Success -> {
                    val cached = result.data
                    _state.update { state ->
                        state.copy(
                            products = UiState.Success(cached.data, cached.isStale),
                            productCategories = if (filter.hasFilters) state.productCategories
                            else categoriesOf(cached.data.map { it.category }),
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(products = UiState.Failure(result.error)) }
            }
        }
    }

    private fun loadServices() {
        servicesJob?.cancel()
        val filter = _state.value.filters.toServiceFilter()
        _state.update { it.copy(services = UiState.Loading) }
        servicesJob = viewModelScope.launch {
            when (val result = serviceRepository.getServices(filter)) {
                is ApiResult.Success -> {
                    val cached = result.data
                    _state.update { state ->
                        state.copy(
                            services = UiState.Success(cached.data, cached.isStale),
                            serviceCategories = if (filter.hasFilters) state.serviceCategories
                            else categoriesOf(cached.data.map { it.category }),
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(services = UiState.Failure(result.error)) }
            }
        }
    }

    private fun categoriesOf(values: List<String>): List<String> =
        values.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
}