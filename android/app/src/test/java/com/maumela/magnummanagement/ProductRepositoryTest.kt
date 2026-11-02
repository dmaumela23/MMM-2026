package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.dao.ProductDao
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.utils.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRepositoryTest {
    private val api = mockk<ApiService>()
    private val dao = mockk<ProductDao>(relaxed = true)
    private val repository = ProductRepository(api, dao)

    private val solar = testProduct(1, "Solar Panel", "Roof panel")
    private val battery = testProduct(2, "Battery", "Storage", category = "Storage")

    private fun stubApi(items: List<com.maumela.magnummanagement.data.model.ProductDto>) {
        coEvery { api.getProducts(any(), any(), any(), any(), any(), any(), any(), any()) } returns items
    }

    private fun stubApiFailure(error: Throwable) {
        coEvery { api.getProducts(any(), any(), any(), any(), any(), any(), any(), any()) } throws error
    }

    // Verifies a normal load returns the server data (not stale) and replaces the cache.
    @Test
    fun getProducts_success_returnsFreshDataAndRefreshesCache() = runTest {
        stubApi(listOf(solar, battery))
        val result = repository.getProducts()
        assertTrue(result is ApiResult.Success)
        val cached = (result as ApiResult.Success).data
        assertEquals(listOf(solar, battery), cached.data)
        assertFalse(cached.isStale)
        coVerify { dao.replaceAll(any()) }
    }

    // Verifies a FILTERED result never wipes the full cache (it only adds/updates rows).
    @Test
    fun getProducts_filtered_upsertsInsteadOfReplacing() = runTest {
        stubApi(listOf(solar))
        repository.getProducts(ListingFilter(query = "solar"))
        coVerify { dao.upsertAll(any()) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    // Verifies the filter choices are sent to the server as the documented query parameters.
    @Test
    fun getProducts_sendsFiltersAsQueryParameters() = runTest {
        stubApi(emptyList())
        repository.getProducts(
            ListingFilter(
                query = "  solar ", category = " Energy ", sector = Sector.ENERGY,
                minPrice = BigDecimal("100.00"), maxPrice = BigDecimal("2000"),
                minRating = 4.0, availableOnly = true,
            ),
        )
        coVerify { api.getProducts("solar", "Energy", "ENERGY", "100.00", "2000", 4.0, true, null) }
    }

    // Verifies that with no network, cached rows are shown, marked stale, and filtered locally.
    @Test
    fun getProducts_networkFailure_fallsBackToFilteredCache() = runTest {
        stubApiFailure(UnknownHostException())
        coEvery { dao.getAll() } returns listOf(solar.toEntity(), battery.toEntity())
        val result = repository.getProducts(ListingFilter(query = "solar"))
        val cached = (result as ApiResult.Success).data
        assertTrue(cached.isStale)
        assertEquals(listOf("Solar Panel"), cached.data.map { it.name })
    }

    // Verifies a filter matching nothing in the cache is an empty list, not an error.
    @Test
    fun getProducts_offlineWithNoMatches_returnsEmptyStaleList() = runTest {
        stubApiFailure(UnknownHostException())
        coEvery { dao.getAll() } returns listOf(solar.toEntity())
        val cached = (repository.getProducts(ListingFilter(query = "zzz")) as ApiResult.Success).data
        assertTrue(cached.isStale)
        assertTrue(cached.data.isEmpty())
    }

    // Verifies no network AND an empty cache reports the real error.
    @Test
    fun getProducts_networkFailureWithEmptyCache_returnsFailure() = runTest {
        stubApiFailure(UnknownHostException())
        coEvery { dao.getAll() } returns emptyList()
        val result = repository.getProducts()
        assertEquals(ErrorKind.NO_INTERNET, (result as ApiResult.Failure).error.kind)
    }

    // Verifies a server error is reported, NOT hidden behind old cached data.
    @Test
    fun getProducts_serverError_doesNotUseCache() = runTest {
        stubApiFailure(httpException(500, """{"detail":"boom"}"""))
        val result = repository.getProducts()
        assertEquals(ErrorKind.SERVER_ERROR, (result as ApiResult.Failure).error.kind)
        coVerify(exactly = 0) { dao.getAll() }
    }

    // Verifies "my products" is never cached and never served from the cache.
    @Test
    fun getProducts_ownerOnly_isNeverCached() = runTest {
        stubApi(listOf(solar))
        repository.getProducts(ListingFilter(ownerOnly = true))
        coVerify { api.getProducts(null, null, null, null, null, null, null, "me") }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
        coVerify(exactly = 0) { dao.upsertAll(any()) }

        stubApiFailure(UnknownHostException())
        val offline = repository.getProducts(ListingFilter(ownerOnly = true))
        assertTrue(offline is ApiResult.Failure)
    }

    // Verifies a cache write failure does not turn a good API response into an error.
    @Test
    fun getProducts_cacheWriteFailure_stillSucceeds() = runTest {
        stubApi(listOf(solar))
        coEvery { dao.replaceAll(any()) } throws RuntimeException("disk full")
        val result = repository.getProducts()
        assertEquals(listOf(solar), (result as ApiResult.Success).data.data)
    }

    // Verifies one product loads from the server and is cached, then falls back when offline.
    @Test
    fun getProduct_cachesAndFallsBackOffline() = runTest {
        coEvery { api.getProduct(1) } returns solar
        assertFalse((repository.getProduct(1) as ApiResult.Success).data.isStale)
        coVerify { dao.upsert(solar.toEntity()) }

        coEvery { api.getProduct(1) } throws UnknownHostException()
        coEvery { dao.getById(1) } returns solar.toEntity()
        val offline = (repository.getProduct(1) as ApiResult.Success).data
        assertTrue(offline.isStale)
        assertEquals(solar, offline.data)
    }

    // Verifies creating a product updates the cache; a 403 for non-business users is passed on.
    @Test
    fun createProduct_successCachesAndForbiddenIsReported() = runTest {
        val request = ProductRequest("Panel", "", BigDecimal("10.00"), "Solar", Sector.ENERGY)
        coEvery { api.createProduct(request) } returns solar
        assertTrue(repository.createProduct(request) is ApiResult.Success)
        coVerify { dao.upsert(solar.toEntity()) }

        coEvery { api.createProduct(request) } throws
                httpException(403, """{"detail":"Only Business accounts can create or manage products."}""")
        val forbidden = repository.createProduct(request) as ApiResult.Failure
        assertEquals(ErrorKind.FORBIDDEN, forbidden.error.kind)
        assertEquals("Only Business accounts can create or manage products.", forbidden.error.message)
    }

    // Verifies a delete removes the cached row, and a 409 (order history) keeps it.
    @Test
    fun deleteProduct_removesFromCacheOnlyOnSuccess() = runTest {
        coEvery { api.deleteProduct(1) } returns com.maumela.magnummanagement.data.model.MessageResponse("Deleted")
        assertTrue(repository.deleteProduct(1) is ApiResult.Success)
        coVerify { dao.deleteById(1) }

        coEvery { api.deleteProduct(2) } throws httpException(409, """{"detail":"This product has order history"}""")
        val conflict = repository.deleteProduct(2) as ApiResult.Failure
        assertEquals(ErrorKind.CONFLICT, conflict.error.kind)
        coVerify(exactly = 0) { dao.deleteById(2) }
    }
}