package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.model.Sector
import java.math.BigDecimal

/** Search/filter choices for products and services. Sent to the server as query parameters. */
data class ListingFilter(
    val query: String? = null,
    val category: String? = null,
    val sector: Sector? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRating: Double? = null,
    val availableOnly: Boolean = false,
    /** true = only my own listings (owner=me). Never cached. */
    val ownerOnly: Boolean = false,
) {
    val hasFilters: Boolean
        get() = !query.isNullOrBlank() || !category.isNullOrBlank() || sector != null ||
                minPrice != null || maxPrice != null || minRating != null || availableOnly
}