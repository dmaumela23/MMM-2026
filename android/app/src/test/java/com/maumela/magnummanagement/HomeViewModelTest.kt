package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.Cached
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val serviceRepository = mockk<ServiceRepository>()

    // Verifies the dashboard shows the logged-in user and only the first four featured items of each kind.
    @Test
    fun load_showsUserAndFourFeaturedItems() {
        val user = testUser(fullName = "Thandi Nkosi")
        every { authRepository.currentUser } returns MutableStateFlow<UserDto?>(user)
        coEvery { productRepository.getProducts(any()) } returns
                ApiResult.Success(Cached((1..6).map { testProduct(it) }, false))
        coEvery { serviceRepository.getServices(any()) } returns
                ApiResult.Success(Cached((1..2).map { testService(it) }, true))

        val viewModel = HomeViewModel(authRepository, productRepository, serviceRepository)

        assertEquals("Thandi Nkosi", viewModel.user.value?.fullName)
        assertEquals(4, (viewModel.state.value.featuredProducts as UiState.Success).data.size)
        val services = viewModel.state.value.featuredServices as UiState.Success
        assertEquals(2, services.data.size)
        assertTrue(services.isStale)
    }

    // Verifies a failure in one list does not stop the other from showing.
    @Test
    fun load_oneListFailing_doesNotBreakTheOther() {
        every { authRepository.currentUser } returns MutableStateFlow<UserDto?>(testUser())
        val error = AppError(ErrorKind.SERVER_UNREACHABLE, "Can't reach the MMM server.")
        coEvery { productRepository.getProducts(any()) } returns ApiResult.Failure(error)
        coEvery { serviceRepository.getServices(any()) } returns
                ApiResult.Success(Cached(listOf(testService(1)), false))

        val viewModel = HomeViewModel(authRepository, productRepository, serviceRepository)

        assertEquals(UiState.Failure(error), viewModel.state.value.featuredProducts)
        assertTrue(viewModel.state.value.featuredServices is UiState.Success)
    }
}