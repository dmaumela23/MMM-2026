package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.dao.OrderDao
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.ItemStatusUpdateRequest
import com.maumela.magnummanagement.data.model.OrderCancelRequest
import com.maumela.magnummanagement.data.model.OrderCreateRequest
import com.maumela.magnummanagement.data.model.OrderItemRequest
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.utils.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {
    private val api = mockk<ApiService>()
    private val dao = mockk<OrderDao>(relaxed = true)
    private val repository = OrderRepository(api, dao)

    // Verifies an empty order is rejected on the device without calling the server.
    @Test
    fun createOrder_empty_isRejectedLocally() = runTest {
        val failure = repository.createOrder(emptyList(), null) as ApiResult.Failure
        assertEquals(ErrorKind.BAD_REQUEST, failure.error.kind)
        coVerify(exactly = 0) { api.createOrder(any()) }
    }

    // Verifies the request carries ids and quantities only, with service quantity forced to 1.
    @Test
    fun createOrder_sendsIdsAndQuantitiesOnly() = runTest {
        val sent = slot<OrderCreateRequest>()
        coEvery { api.createOrder(capture(sent)) } returns testOrder()
        val result = repository.createOrder(
            listOf(OrderItemRequest(productId = 1, quantity = 2), OrderItemRequest(serviceId = 5, quantity = 7)),
            notes = "  Please call first ",
        )
        assertTrue(result is ApiResult.Success)
        assertEquals(
            listOf(OrderItemRequest(productId = 1, quantity = 2), OrderItemRequest(serviceId = 5, quantity = 1)),
            sent.captured.items,
        )
        assertEquals("Please call first", sent.captured.notes)
    }

    // Verifies blank notes are sent as absent, not as an empty string.
    @Test
    fun createOrder_blankNotesBecomeNull() = runTest {
        val sent = slot<OrderCreateRequest>()
        coEvery { api.createOrder(capture(sent)) } returns testOrder()
        repository.createOrder(listOf(OrderItemRequest(serviceId = 5)), "   ")
        assertNull(sent.captured.notes)
    }

    // Verifies the server's out-of-stock message reaches the user.
    @Test
    fun createOrder_insufficientStock_showsServerMessage() = runTest {
        coEvery { api.createOrder(any()) } throws
                httpException(400, """{"detail":"Insufficient stock for 'Battery'. Only 2 left."}""")
        val failure = repository.createOrder(listOf(OrderItemRequest(productId = 1, quantity = 5)), null) as ApiResult.Failure
        assertEquals(ErrorKind.BAD_REQUEST, failure.error.kind)
        assertEquals("Insufficient stock for 'Battery'. Only 2 left.", failure.error.message)
    }

    // Verifies the order list refreshes the cache and falls back to it only when offline.
    @Test
    fun getOrders_cachesAndFallsBackOffline() = runTest {
        val summary = testOrderSummary(1)
        coEvery { api.getOrders() } returns listOf(summary)
        val fresh = (repository.getOrders() as ApiResult.Success).data
        assertEquals(false, fresh.isStale)
        coVerify { dao.replaceAll(listOf(summary.toEntity())) }

        coEvery { api.getOrders() } throws UnknownHostException()
        coEvery { dao.getAll() } returns listOf(summary.toEntity())
        val offline = (repository.getOrders() as ApiResult.Success).data
        assertTrue(offline.isStale)
        assertEquals(listOf(summary), offline.data)
    }

    // Verifies a 401 on the order list is reported (not hidden by cached orders).
    @Test
    fun getOrders_unauthorized_doesNotUseCache() = runTest {
        coEvery { api.getOrders() } throws httpException(401, """{"detail":"Session expired. Please log in again."}""")
        val failure = repository.getOrders() as ApiResult.Failure
        assertEquals(ErrorKind.UNAUTHORIZED, failure.error.kind)
        coVerify(exactly = 0) { dao.getAll() }
    }

    // Verifies cancelling sends status CANCELLED to the right order.
    @Test
    fun cancelOrder_sendsCancelledStatus() = runTest {
        coEvery { api.cancelOrder(4, OrderCancelRequest(OrderStatus.CANCELLED)) } returns testOrder(4)
        assertTrue(repository.cancelOrder(4) is ApiResult.Success)
    }

    // Verifies a seller's status update targets the right order line and a refusal is reported.
    @Test
    fun updateItemStatus_sendsStatusAndReportsInvalidTransition() = runTest {
        val request = ItemStatusUpdateRequest(ItemStatus.COMPLETED)
        coEvery { api.updateItemStatus(4, 9, request) } throws
                httpException(400, """{"detail":"Cannot change item status from PENDING to COMPLETED"}""")
        val failure = repository.updateItemStatus(4, 9, ItemStatus.COMPLETED) as ApiResult.Failure
        assertEquals("Cannot change item status from PENDING to COMPLETED", failure.error.message)
    }
}