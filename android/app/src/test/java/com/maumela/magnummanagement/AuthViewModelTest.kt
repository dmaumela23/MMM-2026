package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.RegisterRequest
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val viewModel = AuthViewModel(repository)
    private val user = testUser()

    // Verifies empty or invalid login fields show errors and never call the server.
    @Test
    fun login_invalidFields_blockedBeforeNetwork() = runTest {
        viewModel.login("", "")
        val state = viewModel.state.value
        assertTrue(state.loginErrors.hasErrors)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    // Verifies a good login finishes loading and reports the user so the screen can navigate.
    @Test
    fun login_success_reportsUserThenCanBeAcknowledged() = runTest {
        coEvery { repository.login("cathy@example.com", "Passw0rd123") } returns ApiResult.Success(user)
        viewModel.login("cathy@example.com", "Passw0rd123")
        assertEquals(user, viewModel.state.value.authenticatedUser)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)

        viewModel.onAuthenticationHandled()
        assertNull(viewModel.state.value.authenticatedUser)
    }

    // Verifies a rejected login shows the server's message and does not log anyone in.
    @Test
    fun login_wrongPassword_showsServerMessage() = runTest {
        coEvery { repository.login(any(), any()) } returns
                ApiResult.Failure(AppError(ErrorKind.UNAUTHORIZED, "Incorrect email or password", 401))
        viewModel.login("cathy@example.com", "WrongPass1")
        assertEquals("Incorrect email or password", viewModel.state.value.errorMessage)
        assertNull(viewModel.state.value.authenticatedUser)
        assertFalse(viewModel.state.value.isLoading)
    }

    // Verifies a second tap while a login is running is ignored (no duplicate requests).
    @Test
    fun login_whileLoading_ignoresSecondTap() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.login(any(), any()) } coAnswers {
            gate.await()
            ApiResult.Success(user)
        }
        viewModel.login("cathy@example.com", "Passw0rd123")
        assertTrue(viewModel.state.value.isLoading)
        viewModel.login("cathy@example.com", "Passw0rd123")
        gate.complete(Unit)
        coVerify(exactly = 1) { repository.login(any(), any()) }
    }

    // Verifies an invalid registration form flags each field and sends nothing.
    @Test
    fun register_invalidForm_blockedBeforeNetwork() = runTest {
        viewModel.register("A", "bad", "abc", "short", "other", UserRole.CUSTOMER)
        val errors = viewModel.state.value.registerErrors
        assertNotNull(errors.fullName)
        assertNotNull(errors.email)
        assertNotNull(errors.phone)
        assertNotNull(errors.password)
        assertNotNull(errors.confirmPassword)
        coVerify(exactly = 0) { repository.register(any()) }
    }

    // Verifies mismatched confirmation is caught on the device.
    @Test
    fun register_passwordMismatch_isReported() = runTest {
        viewModel.register("Thandi Nkosi", "thandi@example.com", "", "Passw0rd123", "Passw0rd999", UserRole.CUSTOMER)
        assertEquals("Passwords do not match", viewModel.state.value.registerErrors.confirmPassword)
        coVerify(exactly = 0) { repository.register(any()) }
    }

    // Verifies a valid registration sends trimmed values, a null phone when blank, and the chosen role.
    @Test
    fun register_success_sendsCleanRequest() = runTest {
        val sent = mutableListOf<RegisterRequest>()
        coEvery { repository.register(capture(sent)) } returns ApiResult.Success(user)
        viewModel.register("  Thandi Nkosi ", " thandi@example.com ", "   ", "Passw0rd123", "Passw0rd123", UserRole.PROVIDER)
        assertEquals(
            RegisterRequest("Thandi Nkosi", "thandi@example.com", null, "Passw0rd123", "Passw0rd123", UserRole.PROVIDER),
            sent.single(),
        )
        assertEquals(user, viewModel.state.value.authenticatedUser)
    }

    // Verifies an email that is already registered shows the server's message.
    @Test
    fun register_duplicateEmail_showsServerMessage() = runTest {
        coEvery { repository.register(any()) } returns
                ApiResult.Failure(AppError(ErrorKind.CONFLICT, "An account with this email already exists", 409))
        viewModel.register("Thandi Nkosi", "thandi@example.com", "", "Passw0rd123", "Passw0rd123", UserRole.CUSTOMER)
        assertEquals("An account with this email already exists", viewModel.state.value.errorMessage)
        assertNull(viewModel.state.value.authenticatedUser)
    }

    // Verifies clearing errors removes both form errors and the server message.
    @Test
    fun clearErrors_resetsEverything() = runTest {
        viewModel.login("", "")
        viewModel.clearErrors()
        assertFalse(viewModel.state.value.loginErrors.hasErrors)
        assertNull(viewModel.state.value.errorMessage)
    }
}