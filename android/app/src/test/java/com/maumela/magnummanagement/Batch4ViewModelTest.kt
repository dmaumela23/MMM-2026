package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.*
import com.maumela.magnummanagement.data.repository.*
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class Batch4ViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun orders_customerLoadsOrders() {
        val auth = mockk<AuthRepository>()
        val repo = mockk<OrderRepository>()
        every { auth.currentUser } returns MutableStateFlow(testUser(role = UserRole.CUSTOMER))
        coEvery { repo.getOrders() } returns ApiResult.Success(Cached(emptyList(), false))
        val vm = OrdersViewModel(auth, repo)
        assertTrue(vm.state.value.orders is UiState.Success)
        assertTrue(vm.state.value.incoming is UiState.Success)
    }

    @Test fun orderDetail_failureBecomesError() {
        val repo = mockk<OrderRepository>()
        val error = AppError(ErrorKind.NOT_FOUND, "Order not found.", 404)
        coEvery { repo.getOrder(7) } returns ApiResult.Failure(error)
        val vm = OrderDetailViewModel(repo, 7)
        assertEquals(UiState.Failure(error), vm.state.value.order)
        assertEquals("Order not found.", vm.state.value.errorMessage)
    }

    @Test fun energy_rejectsInvalidDaySelection() {
        val repo = mockk<EnergyRepository>()
        coEvery { repo.getUsage(30) } returns ApiResult.Failure(AppError(ErrorKind.NO_INTERNET, "Offline"))
        coEvery { repo.getPlans() } returns ApiResult.Failure(AppError(ErrorKind.NO_INTERNET, "Offline"))
        coEvery { repo.getReports() } returns ApiResult.Failure(AppError(ErrorKind.NO_INTERNET, "Offline"))
        val vm = EnergyViewModel(repo)
        vm.setDays(14)
        assertEquals(30, vm.state.value.selectedDays)
    }

    @Test fun profile_invalidFormDoesNotSave() = runTest {
        val auth = mockk<AuthRepository>()
        every { auth.currentUser } returns MutableStateFlow(testUser())
        val vm = ProfileViewModel(auth)
        vm.saveProfile("A", "bad", "x")
        assertTrue(vm.state.value.errors.hasErrors)
        assertFalse(vm.state.value.isSaving)
    }

    @Test fun productDetail_saveSuccessUpdatesState() = runTest {
        val repo = mockk<ProductRepository>()
        val product = testProduct(1)
        coEvery { repo.getProduct(1) } returns ApiResult.Success(Cached(product, false))
        coEvery { repo.updateProduct(1, any()) } returns ApiResult.Success(product)
        val vm = ProductDetailViewModel(repo, 1)
        vm.update(ProductUpdateRequest(name = "Updated"))
        assertTrue(vm.state.value.saved)
    }

    @Test fun serviceDetail_deleteSuccessSetsDeleted() = runTest {
        val repo = mockk<ServiceRepository>()
        val service = testService(2)
        coEvery { repo.getService(2) } returns ApiResult.Success(Cached(service, false))
        coEvery { repo.deleteService(2) } returns ApiResult.Success(Unit)
        val vm = ServiceDetailViewModel(repo, 2)
        vm.delete()
        assertTrue(vm.state.value.deleted)
    }
}
