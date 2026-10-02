package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.SessionState
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.viewmodel.SplashState
import com.maumela.magnummanagement.viewmodel.SplashViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplashViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private fun viewModel() = SplashViewModel(repository, minimumDisplayMillis = 0)

    // Verifies a valid saved login goes straight to Home.
    @Test
    fun activeSession_goesToHome() = runTest {
        coEvery { repository.restoreSession() } returns ApiResult.Success(SessionState.Active(testUser()))
        assertEquals(SplashState.GoToHome, viewModel().state.value)
    }

    // Verifies no saved login (or an expired one) goes to Login.
    @Test
    fun noSession_goesToLogin() = runTest {
        coEvery { repository.restoreSession() } returns ApiResult.Success(SessionState.None)
        assertEquals(SplashState.GoToLogin, viewModel().state.value)
    }

    // Verifies a network problem shows an error with a message instead of logging the user out.
    @Test
    fun serverUnreachable_showsRetryableError() = runTest {
        coEvery { repository.restoreSession() } returns
                ApiResult.Failure(AppError(ErrorKind.SERVER_UNREACHABLE, "Can't reach the MMM server."))
        assertEquals(SplashState.Error("Can't reach the MMM server."), viewModel().state.value)
    }

    // Verifies Retry checks again and can succeed after a failure.
    @Test
    fun retry_afterFailure_canSucceed() = runTest {
        coEvery { repository.restoreSession() } returnsMany listOf(
            ApiResult.Failure(AppError(ErrorKind.NO_INTERNET, "No internet connection.")),
            ApiResult.Success(SessionState.Active(testUser())),
        )
        val viewModel = viewModel()
        assertEquals(SplashState.Error("No internet connection."), viewModel.state.value)
        viewModel.check()
        assertEquals(SplashState.GoToHome, viewModel.state.value)
    }
}