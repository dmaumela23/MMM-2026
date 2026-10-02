package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.repository.ListingFilter
import java.math.BigDecimal

/**
 * Applies the SAME rules as the server's SQL filters to cached rows, so the Marketplace
 * behaves consistently when offline. A pure function: easy to unit test.
 * (When online the server does the filtering; this is only used for the offline fallback.)
 */
object CacheFilters {

    fun filterProducts(items: List<ProductDto>, filter: ListingFilter): List<ProductDto> {
        if (hasInvalidPriceRange(filter)) return emptyList()
        return items.filter {
            matchesCommon(it.name, it.description, it.category, it.sector, it.price, it.rating, filter) &&
                    (!filter.availableOnly || (it.availability && it.stockQuantity > 0))
        }
    }

    fun filterServices(items: List<ServiceDto>, filter: ListingFilter): List<ServiceDto> {
        if (hasInvalidPriceRange(filter)) return emptyList()
        return items.filter {
            matchesCommon(it.name, it.description, it.category, it.sector, it.price, it.rating, filter) &&
                    (!filter.availableOnly || it.availability)
        }
    }

    // The server rejects min_price > max_price with 400; offline there is simply nothing to show.
    private fun hasInvalidPriceRange(filter: ListingFilter): Boolean {
        val min = filter.minPrice
        val max = filter.maxPrice
        return min != null && max != null && min > max
    }

    private fun matchesCommon(
        name: String,
        description: String,
        category: String,
        sector: Sector,
        price: BigDecimal,
        rating: Double,
        filter: ListingFilter,
    ): Boolean {
        val query = filter.query?.trim().orEmpty()
        if (query.isNotEmpty() &&
            !name.contains(query, ignoreCase = true) &&
            !description.contains(query, ignoreCase = true)
        ) return false

        val wantedCategory = filter.category?.trim().orEmpty()
        if (wantedCategory.isNotEmpty() && !category.equals(wantedCategory, ignoreCase = true)) return false

        if (filter.sector != null && sector != filter.sector) return false
        if (filter.minPrice != null && price < filter.minPrice) return false
        if (filter.maxPrice != null && price > filter.maxPrice) return false
        if (filter.minRating != null && rating < filter.minRating) return false
        return true
    }
}