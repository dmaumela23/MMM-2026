package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.dao.ServiceDao
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.repository.ListingFilter
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRepositoryTest {
    private val api = mockk<ApiService>()
    private val dao = mockk<ServiceDao>(relaxed = true)
    private val repository = ServiceRepository(api, dao)

    private val install = testService(1, "Solar Installation", "Roof work")
    private val design = testService(2, "Website Design", "Responsive site", category = "Web")

    // Verifies a normal load returns fresh server data and replaces the cache.
    @Test
    fun getServices_success_refreshesCache() = runTest {
        coEvery { api.getServices(any(), any(), any(), any(), any(), any(), any(), any()) } returns listOf(install, design)
        val cached = (repository.getServices() as ApiResult.Success).data
        assertFalse(cached.isStale)
        assertEquals(2, cached.data.size)
        coVerify { dao.replaceAll(any()) }
    }

    // Verifies a server timeout shows cached services, filtered locally and marked stale.
    @Test
    fun getServices_timeout_fallsBackToFilteredCache() = runTest {
        coEvery { api.getServices(any(), any(), any(), any(), any(), any(), any(), any()) } throws SocketTimeoutException()
        coEvery { dao.getAll() } returns listOf(install.toEntity(), design.toEntity())
        val cached = (repository.getServices(ListingFilter(category = "web")) as ApiResult.Success).data
        assertTrue(cached.isStale)
        assertEquals(listOf("Website Design"), cached.data.map { it.name })
    }

    // Verifies a 404 for one service is reported rather than hidden by the cache.
    @Test
    fun getService_notFound_isReported() = runTest {
        coEvery { api.getService(99) } throws httpException(404, """{"detail":"Service not found"}""")
        val failure = repository.getService(99) as ApiResult.Failure
        assertEquals(ErrorKind.NOT_FOUND, failure.error.kind)
        assertEquals("Service not found", failure.error.message)
        coVerify(exactly = 0) { dao.getById(any()) }
    }
}