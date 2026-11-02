package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.repository.Cached
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.MarketplaceTab
import com.maumela.magnummanagement.viewmodel.MarketplaceViewModel
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MarketplaceViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val productRepository = mockk<ProductRepository>()
    private val serviceRepository = mockk<ServiceRepository>()

    private val products = listOf(
        testProduct(1, "Solar Panel", category = "Solar"),
        testProduct(2, "Battery", category = "Storage"),
        testProduct(3, "Panel Mount", category = "solar"),
    )
    private val services = listOf(
        testService(1, "Installation", category = "Installation"),
        testService(2, "Website Design", category = "Web"),
    )
    private val productFilters = mutableListOf<ListingFilter>()
    private val serviceFilters = mutableListOf<ListingFilter>()

    private fun stubSuccess(stale: Boolean = false) {
        coEvery { productRepository.getProducts(capture(productFilters)) } returns ApiResult.Success(Cached(products, stale))
        coEvery { serviceRepository.getServices(capture(serviceFilters)) } returns ApiResult.Success(Cached(services, stale))
    }

    private fun newViewModel() = MarketplaceViewModel(productRepository, serviceRepository)

    // Verifies the first open loads both lists from the repositories with no filters.
    @Test
    fun ensureLoaded_loadsBothListsUnfiltered() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        val state = viewModel.state.value
        assertEquals(3, (state.products as UiState.Success).data.size)
        assertEquals(2, (state.services as UiState.Success).data.size)
        assertFalse((state.products as UiState.Success).isStale)
        assertEquals(ListingFilter(), productFilters.single())
        assertEquals(ListingFilter(), serviceFilters.single())
    }

    // Verifies calling ensureLoaded again does not reload.
    @Test
    fun ensureLoaded_onlyLoadsOnce() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.ensureLoaded()
        assertEquals(1, productFilters.size)
    }

    // Verifies data served from the offline cache is marked stale so the banner can show.
    @Test
    fun staleData_isFlagged() {
        stubSuccess(stale = true)
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        assertTrue((viewModel.state.value.products as UiState.Success).isStale)
        assertTrue((viewModel.state.value.services as UiState.Success).isStale)
    }

    // Verifies a failed request becomes an error state with a friendly message, never a crash.
    @Test
    fun failure_becomesErrorState() {
        val error = AppError(ErrorKind.NO_INTERNET, "No internet connection.")
        coEvery { productRepository.getProducts(any()) } returns ApiResult.Failure(error)
        coEvery { serviceRepository.getServices(any()) } returns ApiResult.Failure(error)
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        assertEquals(UiState.Failure(error), viewModel.state.value.products)
        assertEquals(UiState.Failure(error), viewModel.state.value.services)
    }

    // Verifies typing alone does not search; submitting does, with the text trimmed.
    @Test
    fun search_runsOnSubmitWithTrimmedText() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.setQuery("  solar ")
        assertEquals(1, productFilters.size)
        viewModel.submitSearch()
        assertEquals("solar", productFilters.last().query)
        assertEquals("solar", serviceFilters.last().query)
    }

    // Verifies clearing the search reloads without a query.
    @Test
    fun clearSearch_reloadsWithoutQuery() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.setQuery("solar")
        viewModel.submitSearch()
        viewModel.clearSearch()
        assertNull(productFilters.last().query)
        assertEquals("", viewModel.state.value.filters.query)
    }

    // Verifies opening from a Home tile selects the tab and runs the search.
    @Test
    fun openWith_selectsTabAndSearches() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.openWith(MarketplaceTab.SERVICES, "solar")
        assertEquals(MarketplaceTab.SERVICES, viewModel.state.value.tab)
        assertEquals("solar", serviceFilters.last().query)
        assertEquals("solar", productFilters.last().query)
    }

    // Verifies opening again with the same arguments does not reload.
    @Test
    fun openWith_sameArguments_doesNotReload() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.openWith(MarketplaceTab.SERVICES, "solar")
        viewModel.openWith(MarketplaceTab.SERVICES, "solar")
        assertEquals(1, productFilters.size)
    }

    // Verifies category chips come from the unfiltered load (distinct, sorted, case-insensitive).
    @Test
    fun categories_comeFromUnfilteredLoad() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        assertEquals(listOf("Solar", "Storage"), viewModel.state.value.productCategories)
        assertEquals(listOf("Installation", "Web"), viewModel.state.value.serviceCategories)
    }

    // Verifies choosing a category reloads only that tab, and the chips do not shrink to the filtered result.
    @Test
    fun productCategory_reloadsProductsOnlyAndKeepsChips() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        coEvery { productRepository.getProducts(capture(productFilters)) } returns
                ApiResult.Success(Cached(listOf(products[1]), false))

        viewModel.setProductCategory("Storage")

        assertEquals("Storage", productFilters.last().category)
        assertEquals(1, serviceFilters.size) // services were not reloaded
        assertEquals(listOf("Solar", "Storage"), viewModel.state.value.productCategories)
        assertEquals(1, (viewModel.state.value.products as UiState.Success).data.size)
    }

    // Verifies an invalid price is rejected with a message and nothing is reloaded.
    @Test
    fun applyFilters_invalidPrice_isRejected() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        assertFalse(viewModel.applyFilters(null, "abc", "", null, false))
        assertNotNull(viewModel.state.value.filterError)
        assertFalse(viewModel.applyFilters(null, "500", "100", null, false))
        assertEquals("Minimum price cannot be higher than the maximum", viewModel.state.value.filterError)
        assertEquals(1, productFilters.size)
    }

    // Verifies valid filters are sent to the server for both lists.
    @Test
    fun applyFilters_valid_sendsThemToTheServer() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        assertTrue(viewModel.applyFilters(Sector.ENERGY, "1000", "5000", 4.0, true))
        assertNull(viewModel.state.value.filterError)
        val expected = ListingFilter(
            sector = Sector.ENERGY, minPrice = BigDecimal("1000"), maxPrice = BigDecimal("5000"),
            minRating = 4.0, availableOnly = true,
        )
        assertEquals(expected, productFilters.last())
        assertEquals(expected, serviceFilters.last())
        assertEquals(5, viewModel.state.value.filters.activeFilterCount)
    }

    // Verifies clearing filters keeps the search text but removes everything else.
    @Test
    fun clearFilters_keepsTheSearchText() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.setQuery("solar")
        viewModel.applyFilters(Sector.ENERGY, "", "", 4.0, true)
        viewModel.clearFilters()
        assertEquals(ListingFilter(query = "solar"), productFilters.last())
        assertEquals(0, viewModel.state.value.filters.activeFilterCount)
    }

    // Verifies switching tabs only changes the selected tab (no extra requests).
    @Test
    fun setTab_doesNotReload() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.setTab(MarketplaceTab.SERVICES)
        assertEquals(MarketplaceTab.SERVICES, viewModel.state.value.tab)
        assertEquals(1, productFilters.size)
    }

    // Verifies refresh reloads both lists.
    @Test
    fun refresh_reloadsBothLists() {
        stubSuccess()
        val viewModel = newViewModel()
        viewModel.ensureLoaded()
        viewModel.refresh()
        assertEquals(2, productFilters.size)
        assertEquals(2, serviceFilters.size)
    }
}
