package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.utils.CacheFilters
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheFiltersTest {
    private val solar = testProduct(1, "Solar Panel 550W", "Monocrystalline panel", "1499.00", "Solar", Sector.ENERGY, 10, true, 4.5)
    private val backup = testProduct(2, "Cloud Backup", "Backup for 100% of your files", "299.00", "Software", Sector.DIGITAL, 50, true, 3.0)
    private val battery = testProduct(3, "Battery Pack", "Lithium storage", "9999.00", "Storage", Sector.ENERGY, 0, true, 4.8)
    private val legacy = testProduct(4, "Legacy Kit", "Old stock", "50.00", "Solar", Sector.ENERGY, 5, false, 0.0)
    private val all = listOf(solar, backup, battery, legacy)

    private fun names(filter: ListingFilter) = CacheFilters.filterProducts(all, filter).map { it.name }.sorted()

    // Verifies an empty filter returns everything.
    @Test
    fun noFilter_returnsEverything() {
        assertEquals(4, CacheFilters.filterProducts(all, ListingFilter()).size)
    }

    // Verifies search is case-insensitive and covers name and description, like the server.
    @Test
    fun query_matchesNameAndDescriptionIgnoringCase() {
        assertEquals(listOf("Solar Panel 550W"), names(ListingFilter(query = "SOLAR")))
        assertEquals(listOf("Battery Pack"), names(ListingFilter(query = "lithium")))
    }

    // Verifies % is treated as a literal character, never as a wildcard.
    @Test
    fun query_treatsPercentLiterally() {
        assertEquals(listOf("Cloud Backup"), names(ListingFilter(query = "%")))
    }

    // Verifies category matching ignores case.
    @Test
    fun category_isCaseInsensitive() {
        assertEquals(listOf("Legacy Kit", "Solar Panel 550W"), names(ListingFilter(category = "solar")))
    }

    // Verifies price bounds are inclusive and can be combined.
    @Test
    fun priceRange_isInclusive() {
        assertEquals(listOf("Battery Pack", "Solar Panel 550W"), names(ListingFilter(minPrice = BigDecimal("1499.00"))))
        assertEquals(listOf("Cloud Backup", "Legacy Kit"), names(ListingFilter(maxPrice = BigDecimal("299"))))
        assertEquals(
            listOf("Solar Panel 550W"),
            names(ListingFilter(minPrice = BigDecimal("300"), maxPrice = BigDecimal("2000"))),
        )
    }

    // Verifies min price above max price shows nothing (the server answers 400 for this).
    @Test
    fun invertedPriceRange_returnsNothing() {
        assertEquals(emptyList<String>(), names(ListingFilter(minPrice = BigDecimal("500"), maxPrice = BigDecimal("100"))))
    }

    // Verifies the rating filter keeps ratings at or above the minimum.
    @Test
    fun minRating_filtersLowerRatings() {
        assertEquals(listOf("Battery Pack", "Solar Panel 550W"), names(ListingFilter(minRating = 4.0)))
    }

    // Verifies "available" means the flag is on AND stock is above zero.
    @Test
    fun availableOnly_requiresFlagAndStock() {
        assertEquals(listOf("Cloud Backup", "Solar Panel 550W"), names(ListingFilter(availableOnly = true)))
    }

    // Verifies sector filtering and combining several filters.
    @Test
    fun sectorAndCombinedFilters() {
        assertEquals(listOf("Cloud Backup"), names(ListingFilter(sector = Sector.DIGITAL)))
        assertEquals(listOf("Solar Panel 550W"), names(ListingFilter(category = "solar", availableOnly = true)))
    }

    // Verifies services use only their availability flag (they have no stock).
    @Test
    fun services_availableOnlyUsesFlag() {
        val services = listOf(testService(1, "Design", availability = true), testService(2, "Audit", availability = false))
        val result = CacheFilters.filterServices(services, ListingFilter(availableOnly = true))
        assertEquals(listOf("Design"), result.map { it.name })
    }
}