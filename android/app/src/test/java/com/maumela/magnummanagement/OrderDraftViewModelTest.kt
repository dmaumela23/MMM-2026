package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.ItemType
import com.maumela.magnummanagement.data.model.OrderItemRequest
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.viewmodel.AddResult
import com.maumela.magnummanagement.viewmodel.DraftItem
import com.maumela.magnummanagement.viewmodel.OrderDraftViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderDraftViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<OrderRepository>()
    private val viewModel = OrderDraftViewModel(repository)

    private val productKey = DraftItem.keyOf(ItemType.PRODUCT, 1)
    private val serviceKey = DraftItem.keyOf(ItemType.SERVICE, 5)

    // Verifies adding a product creates a line with the product's price and a stock-limited maximum.
    @Test
    fun addProduct_createsLine() {
        val result = viewModel.addProduct(testProduct(1, price = "100.00", stock = 3), quantity = 2)
        assertEquals(AddResult.ADDED, result)
        val item = viewModel.state.value.items.single()
        assertEquals(2, item.quantity)
        assertEquals(3, item.maxQuantity)
        assertEquals(BigDecimal("100.00"), item.unitPrice)
    }

    // Verifies adding the same product again raises its quantity, until the stock limit is reached.
    @Test
    fun addProduct_again_increasesQuantityUntilStockLimit() {
        val product = testProduct(1, stock = 3)
        assertEquals(AddResult.ADDED, viewModel.addProduct(product))
        assertEquals(AddResult.QUANTITY_INCREASED, viewModel.addProduct(product))
        assertEquals(AddResult.QUANTITY_INCREASED, viewModel.addProduct(product))
        assertEquals(3, viewModel.state.value.items.single().quantity)
        assertEquals(AddResult.LIMIT_REACHED, viewModel.addProduct(product))
        assertEquals(3, viewModel.state.value.items.single().quantity)
    }

    // Verifies unavailable and out-of-stock products cannot be added.
    @Test
    fun addProduct_unavailableOrSoldOut_isRejected() {
        assertEquals(AddResult.UNAVAILABLE, viewModel.addProduct(testProduct(1, availability = false)))
        assertEquals(AddResult.UNAVAILABLE, viewModel.addProduct(testProduct(2, stock = 0)))
        assertTrue(viewModel.state.value.isEmpty)
    }

    // Verifies a service is added once with quantity 1, and unavailable services are refused.
    @Test
    fun addService_onceOnlyWithQuantityOne() {
        val service = testService(5)
        assertEquals(AddResult.ADDED, viewModel.addService(service))
        assertEquals(AddResult.ALREADY_IN_ORDER, viewModel.addService(service))
        assertEquals(1, viewModel.state.value.items.single().quantity)
        assertEquals(AddResult.UNAVAILABLE, viewModel.addService(testService(6, availability = false)))
        assertEquals(1, viewModel.state.value.itemCount)
    }

    // Verifies quantity changes stay within 1 and the line's maximum, and services stay at 1.
    @Test
    fun setQuantity_isClamped() {
        viewModel.addProduct(testProduct(1, stock = 3))
        viewModel.addService(testService(5))
        viewModel.setQuantity(productKey, 999)
        assertEquals(3, viewModel.state.value.items.first { it.key == productKey }.quantity)
        viewModel.setQuantity(productKey, 0)
        assertEquals(1, viewModel.state.value.items.first { it.key == productKey }.quantity)
        viewModel.setQuantity(serviceKey, 4)
        assertEquals(1, viewModel.state.value.items.first { it.key == serviceKey }.quantity)
    }

    // Verifies the previewed total is exact: 3 x R100.00 + R250.50 = R550.50.
    @Test
    fun total_isExactForMixedOrder() {
        viewModel.addProduct(testProduct(1, price = "100.00", stock = 10), quantity = 3)
        viewModel.addService(testService(5, price = "250.50"))
        assertEquals(BigDecimal("550.50"), viewModel.state.value.total)
    }

    // Verifies removing a line and clearing the draft update the state.
    @Test
    fun removeAndClear() {
        viewModel.addProduct(testProduct(1))
        viewModel.addService(testService(5))
        viewModel.remove(productKey)
        assertEquals(listOf(serviceKey), viewModel.state.value.items.map { it.key })
        viewModel.setNotes("Call first")
        viewModel.clear()
        assertTrue(viewModel.state.value.isEmpty)
        assertEquals("", viewModel.state.value.notes)
        assertEquals(BigDecimal("0.00"), viewModel.state.value.total)
    }

    // Verifies submitting an empty draft shows a message and sends nothing.
    @Test
    fun submit_emptyDraft_isRejected() = runTest {
        viewModel.submit()
        assertNotNull(viewModel.state.value.errorMessage)
        coVerify(exactly = 0) { repository.createOrder(any(), any()) }
    }

    // Verifies over-long notes block the submit with a field error.
    @Test
    fun submit_longNotes_isBlocked() = runTest {
        viewModel.addService(testService(5))
        viewModel.setNotes("x".repeat(1001))
        viewModel.submit()
        assertNotNull(viewModel.state.value.notesError)
        assertFalse(viewModel.state.value.isSubmitting)
        coVerify(exactly = 0) { repository.createOrder(any(), any()) }
    }

    // Verifies a successful submit sends ids and quantities only, clears the draft and reports the new order.
    @Test
    fun submit_success_sendsIdsOnlyAndClearsDraft() = runTest {
        val expected = listOf(OrderItemRequest(productId = 1, quantity = 2), OrderItemRequest(serviceId = 5, quantity = 1))
        coEvery { repository.createOrder(expected, "Call first") } returns ApiResult.Success(testOrder(id = 42))
        viewModel.addProduct(testProduct(1, stock = 10), quantity = 2)
        viewModel.addService(testService(5))
        viewModel.setNotes("Call first")

        viewModel.submit()

        coVerify { repository.createOrder(expected, "Call first") }
        val state = viewModel.state.value
        assertEquals(42, state.createdOrderId)
        assertTrue(state.isEmpty)
        assertFalse(state.isSubmitting)

        viewModel.onOrderCreatedHandled()
        assertNull(viewModel.state.value.createdOrderId)
    }

    // Verifies a server refusal (for example out of stock) keeps the draft and shows the server's message.
    @Test
    fun submit_serverRefusal_keepsDraftAndShowsMessage() = runTest {
        coEvery { repository.createOrder(any(), any()) } returns
                ApiResult.Failure(AppError(ErrorKind.BAD_REQUEST, "Insufficient stock for 'Solar Panel'. Only 1 left.", 400))
        viewModel.addProduct(testProduct(1, stock = 10), quantity = 5)

        viewModel.submit()

        val state = viewModel.state.value
        assertEquals("Insufficient stock for 'Solar Panel'. Only 1 left.", state.errorMessage)
        assertEquals(1, state.itemCount)
        assertFalse(state.isSubmitting)
        assertNull(state.createdOrderId)
    }
}