package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.Cached
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.OrdersViewModel
import com.maumela.magnummanagement.viewmodel.ProfileOverviewViewModel
import com.maumela.magnummanagement.viewmodel.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdersRolesTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()
    private val orders = mockk<OrderRepository>()

    private fun viewModel(role: UserRole): OrdersViewModel {
        every { auth.currentUser } returns MutableStateFlow<UserDto?>(testUser(role = role))
        coEvery { orders.getOrders() } returns ApiResult.Success(Cached(listOf(testOrderSummary(1)), false))
        coEvery { orders.getIncomingItems() } returns ApiResult.Success(emptyList())
        return OrdersViewModel(auth, orders)
    }

    // Verifies a Provider also sees the orders they PLACED, plus the incoming requests (this was a bug before).
    @Test
    fun provider_seesOwnOrdersAndIncoming() {
        val vm = viewModel(UserRole.PROVIDER)
        assertEquals(1, (vm.state.value.orders as UiState.Success).data.size)
        assertTrue(vm.state.value.incoming is UiState.Success)
        coVerify(exactly = 1) { orders.getOrders() }
        coVerify(exactly = 1) { orders.getIncomingItems() }
    }

    // Verifies a Business account loads both lists too.
    @Test
    fun business_loadsBothLists() {
        viewModel(UserRole.BUSINESS)
        coVerify { orders.getOrders() }
        coVerify { orders.getIncomingItems() }
    }

    // Verifies a Customer never asks for incoming requests (the server would answer 403).
    @Test
    fun customer_neverLoadsIncoming() {
        val vm = viewModel(UserRole.CUSTOMER)
        assertEquals(1, (vm.state.value.orders as UiState.Success).data.size)
        coVerify(exactly = 0) { orders.getIncomingItems() }
    }

    // Verifies a failing incoming list does not hide the user's own orders.
    @Test
    fun incomingFailure_keepsOwnOrders() {
        every { auth.currentUser } returns MutableStateFlow<UserDto?>(testUser(role = UserRole.BUSINESS))
        val error = AppError(ErrorKind.SERVER_UNREACHABLE, "Can't reach the MMM server.")
        coEvery { orders.getOrders() } returns ApiResult.Success(Cached(listOf(testOrderSummary(1)), false))
        coEvery { orders.getIncomingItems() } returns ApiResult.Failure(error)
        val vm = OrdersViewModel(auth, orders)
        assertTrue(vm.state.value.orders is UiState.Success)
        assertEquals(UiState.Failure(error), vm.state.value.incoming)
    }
}

class ProfileOverviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()
    private val orders = mockk<OrderRepository>()
    private val products = mockk<ProductRepository>()
    private val services = mockk<ServiceRepository>()
    private val productRequest = ProductRequest("Panel", "", BigDecimal("10.00"), "Solar", Sector.ENERGY)

    private fun viewModel(role: UserRole): ProfileOverviewViewModel {
        every { auth.currentUser } returns MutableStateFlow<UserDto?>(testUser(role = role))
        coEvery { orders.getOrders() } returns
            ApiResult.Success(Cached(listOf(testOrderSummary(1), testOrderSummary(2)), false))
        return ProfileOverviewViewModel(auth, orders, products, services)
    }

    // Verifies a Customer sees only an order count, and no listing requests are made (unstubbed calls would fail).
    @Test
    fun customer_loadsOnlyOrderCount() {
        val vm = viewModel(UserRole.CUSTOMER)
        assertEquals(2, vm.state.value.orderCount)
        assertEquals(UiState.Success(emptyList<Any>()), vm.state.value.products)
        assertEquals(UiState.Success(emptyList<Any>()), vm.state.value.services)
    }

    // Verifies a Provider loads their own services only.
    @Test
    fun provider_loadsOwnServices() {
        coEvery { services.getServices(any()) } returns ApiResult.Success(Cached(listOf(testService(1)), false))
        val vm = viewModel(UserRole.PROVIDER)
        assertEquals(1, (vm.state.value.services as UiState.Success).data.size)
        assertEquals(UiState.Success(emptyList<Any>()), vm.state.value.products)
    }

    // Verifies a Business loads its own products and services, asking the server for "owner = me" lists.
    @Test
    fun business_loadsOwnProductsAndServices() {
        coEvery { products.getProducts(any()) } returns ApiResult.Success(Cached(listOf(testProduct(1), testProduct(2)), false))
        coEvery { services.getServices(any()) } returns ApiResult.Success(Cached(listOf(testService(1)), false))
        val vm = viewModel(UserRole.BUSINESS)
        assertEquals(2, (vm.state.value.products as UiState.Success).data.size)
        assertEquals(1, (vm.state.value.services as UiState.Success).data.size)
        coVerify { products.getProducts(match { it.ownerOnly }) }
        coVerify { services.getServices(match { it.ownerOnly }) }
    }

    // Verifies adding a product reports success and reloads the list.
    @Test
    fun createProduct_success_reloadsAndReportsSaved() {
        coEvery { products.getProducts(any()) } returns ApiResult.Success(Cached(listOf(testProduct(1)), false))
        coEvery { services.getServices(any()) } returns ApiResult.Success(Cached(emptyList(), false))
        coEvery { products.createProduct(productRequest) } returns ApiResult.Success(testProduct(7))
        val vm = viewModel(UserRole.BUSINESS)

        vm.createProduct(productRequest)

        assertEquals("Product added", vm.state.value.savedMessage)
        assertFalse(vm.state.value.isSaving)
        coVerify(exactly = 2) { products.getProducts(any()) }
        vm.clearMessages()
        assertNull(vm.state.value.savedMessage)
    }

    // Verifies a refusal from the server (for example 403) shows its message and nothing is reported saved.
    @Test
    fun createProduct_forbidden_showsServerMessage() {
        coEvery { products.getProducts(any()) } returns ApiResult.Success(Cached(emptyList(), false))
        coEvery { services.getServices(any()) } returns ApiResult.Success(Cached(emptyList(), false))
        coEvery { products.createProduct(any()) } returns ApiResult.Failure(
            AppError(ErrorKind.FORBIDDEN, "Only Business accounts can create or manage products.", 403),
        )
        val vm = viewModel(UserRole.BUSINESS)

        vm.createProduct(productRequest)

        assertEquals("Only Business accounts can create or manage products.", vm.state.value.errorMessage)
        assertNull(vm.state.value.savedMessage)
        assertFalse(vm.state.value.isSaving)
    }
}

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()

    private fun viewModel(): ProfileViewModel {
        every { auth.currentUser } returns MutableStateFlow<UserDto?>(testUser())
        return ProfileViewModel(auth)
    }

    // Verifies a valid profile edit is sent (blank phone as null) and reported as saved.
    @Test
    fun saveProfile_success() {
        coEvery { auth.updateProfile(1, "Cathy Updated", "cathy@example.com", null) } returns ApiResult.Success(testUser())
        val vm = viewModel()
        vm.saveProfile("Cathy Updated", "cathy@example.com", "  ")
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.isSaving)
    }

    // Verifies an email that belongs to someone else shows the server's 409 message.
    @Test
    fun saveProfile_emailTaken_showsServerMessage() {
        coEvery { auth.updateProfile(any(), any(), any(), any()) } returns ApiResult.Failure(
            AppError(ErrorKind.CONFLICT, "An account with this email already exists", 409),
        )
        val vm = viewModel()
        vm.saveProfile("Cathy", "taken@example.com", "")
        assertEquals("An account with this email already exists", vm.state.value.errorMessage)
        assertFalse(vm.state.value.saved)
    }

    // Verifies password rules are enforced on the device before any request is sent.
    @Test
    fun changePassword_invalid_isBlocked() {
        val vm = viewModel()
        vm.changePassword("Old12345", "weak", "weak")
        assertTrue(vm.state.value.passwordErrors.hasErrors)
        coVerify(exactly = 0) { auth.changePassword(any(), any(), any()) }
    }

    // Verifies a successful password change is reported.
    @Test
    fun changePassword_success() {
        coEvery { auth.changePassword(1, "Old12345", "New12345") } returns ApiResult.Success(Unit)
        val vm = viewModel()
        vm.changePassword("Old12345", "New12345", "New12345")
        assertTrue(vm.state.value.passwordChanged)
    }

    // Verifies a wrong current password shows the server's message.
    @Test
    fun changePassword_wrongCurrent_showsServerMessage() {
        coEvery { auth.changePassword(any(), any(), any()) } returns ApiResult.Failure(
            AppError(ErrorKind.BAD_REQUEST, "Current password is incorrect", 400),
        )
        val vm = viewModel()
        vm.changePassword("Wrong123", "New12345", "New12345")
        assertEquals("Current password is incorrect", vm.state.value.errorMessage)
        assertFalse(vm.state.value.passwordChanged)
    }
}
