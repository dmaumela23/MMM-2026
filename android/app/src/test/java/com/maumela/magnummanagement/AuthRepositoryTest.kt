package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.local.TokenStore
import com.maumela.magnummanagement.data.model.AuthResponse
import com.maumela.magnummanagement.data.model.LoginRequest
import com.maumela.magnummanagement.data.model.MessageResponse
import com.maumela.magnummanagement.data.model.PasswordChangeRequest
import com.maumela.magnummanagement.data.model.RegisterRequest
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.data.model.UserUpdateRequest
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.SessionState
import com.maumela.magnummanagement.utils.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.ConnectException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    private val api = mockk<ApiService>()
    private val tokenStore = mockk<TokenStore>(relaxed = true)
    private val preferences = mockk<PreferencesStore>(relaxed = true)
    private var localDataCleared = false
    private val repository = AuthRepository(api, tokenStore, preferences) { localDataCleared = true }

    private val user = testUser()

    // Verifies a successful login stores the token securely and exposes the user.
    @Test
    fun login_success_savesTokenAndSetsCurrentUser() = runTest {
        coEvery { api.login(LoginRequest("cathy@example.com", "Passw0rd123")) } returns AuthResponse("jwt-token", "bearer", user)
        val result = repository.login("  cathy@example.com ", "Passw0rd123")
        assertEquals(ApiResult.Success(user), result)
        coVerify { tokenStore.save("jwt-token") }
        assertEquals(user, repository.currentUser.value)
    }

    // Verifies a wrong password shows the server message and stores nothing.
    @Test
    fun login_wrongPassword_failsWithoutSavingToken() = runTest {
        coEvery { api.login(any()) } throws httpException(401, """{"detail":"Incorrect email or password"}""")
        val failure = repository.login("cathy@example.com", "wrong") as ApiResult.Failure
        assertEquals(ErrorKind.UNAUTHORIZED, failure.error.kind)
        assertEquals("Incorrect email or password", failure.error.message)
        coVerify(exactly = 0) { tokenStore.save(any()) }
        assertNull(repository.currentUser.value)
    }

    // Verifies registration saves the token and a duplicate email shows the 409 message.
    @Test
    fun register_successAndDuplicateEmail() = runTest {
        val request = RegisterRequest("Cathy", "cathy@example.com", null, "Passw0rd123", "Passw0rd123", UserRole.CUSTOMER)
        coEvery { api.register(request) } returns AuthResponse("jwt-token", "bearer", user)
        assertTrue(repository.register(request) is ApiResult.Success)
        coVerify { tokenStore.save("jwt-token") }

        coEvery { api.register(request) } throws httpException(409, """{"detail":"An account with this email already exists"}""")
        val failure = repository.register(request) as ApiResult.Failure
        assertEquals(ErrorKind.CONFLICT, failure.error.kind)
    }

    // Verifies a failure to store the token securely is reported and nobody is logged in.
    @Test
    fun login_tokenStorageFailure_isReported() = runTest {
        coEvery { api.login(any()) } returns AuthResponse("jwt-token", "bearer", user)
        coEvery { tokenStore.save(any()) } throws IllegalStateException("keystore unavailable")
        val failure = repository.login("cathy@example.com", "Passw0rd123") as ApiResult.Failure
        assertTrue(failure.error.message.contains("securely"))
        assertNull(repository.currentUser.value)
    }

    // Verifies Splash goes to Login when nothing is stored, without calling the server.
    @Test
    fun restoreSession_withoutToken_returnsNone() = runTest {
        coEvery { tokenStore.load() } returns null
        assertEquals(ApiResult.Success(SessionState.None), repository.restoreSession())
        coVerify(exactly = 0) { api.me() }
    }

    // Verifies a valid stored token restores the session.
    @Test
    fun restoreSession_validToken_returnsActive() = runTest {
        coEvery { tokenStore.load() } returns "jwt-token"
        coEvery { api.me() } returns user
        assertEquals(ApiResult.Success(SessionState.Active(user)), repository.restoreSession())
        assertEquals(user, repository.currentUser.value)
    }

    // Verifies an expired token is cleared and the user goes to Login.
    @Test
    fun restoreSession_expiredToken_logsOut() = runTest {
        coEvery { tokenStore.load() } returns "old-token"
        coEvery { api.me() } throws httpException(401, """{"detail":"Session expired. Please log in again."}""")
        assertEquals(ApiResult.Success(SessionState.None), repository.restoreSession())
        coVerify { tokenStore.clear() }
        assertTrue(localDataCleared)
    }

    // Verifies a network problem keeps the token so the user can simply retry.
    @Test
    fun restoreSession_networkFailure_keepsToken() = runTest {
        coEvery { tokenStore.load() } returns "jwt-token"
        coEvery { api.me() } throws ConnectException()
        val failure = repository.restoreSession() as ApiResult.Failure
        assertEquals(ErrorKind.SERVER_UNREACHABLE, failure.error.kind)
        coVerify(exactly = 0) { tokenStore.clear() }
    }

    // Verifies logout removes the token, preferences, cached data and current user.
    @Test
    fun logout_clearsEverything() = runTest {
        coEvery { api.login(any()) } returns AuthResponse("jwt-token", "bearer", user)
        repository.login("cathy@example.com", "Passw0rd123")
        repository.logout()
        coVerify { tokenStore.clear() }
        coVerify { preferences.clear() }
        assertTrue(localDataCleared)
        assertNull(repository.currentUser.value)
    }

    // Verifies a profile update replaces the current user, and a 409 email clash is reported.
    @Test
    fun updateProfile_updatesCurrentUserAndReportsConflict() = runTest {
        val updated = user.copy(fullName = "Cathy Updated")
        coEvery { api.updateUser(1, UserUpdateRequest("Cathy Updated", null, null)) } returns updated
        repository.updateProfile(1, fullName = " Cathy Updated ")
        assertEquals(updated, repository.currentUser.value)

        coEvery { api.updateUser(1, UserUpdateRequest(null, "taken@example.com", null)) } throws
                httpException(409, """{"detail":"An account with this email already exists"}""")
        val failure = repository.updateProfile(1, email = "taken@example.com") as ApiResult.Failure
        assertEquals(ErrorKind.CONFLICT, failure.error.kind)
    }

    // Verifies the password change sends both passwords and reports a wrong current password.
    @Test
    fun changePassword_successAndWrongCurrentPassword() = runTest {
        coEvery { api.changePassword(1, PasswordChangeRequest("Old12345", "New12345")) } returns MessageResponse("Password updated")
        assertEquals(ApiResult.Success(Unit), repository.changePassword(1, "Old12345", "New12345"))

        coEvery { api.changePassword(1, PasswordChangeRequest("Wrong123", "New12345")) } throws
                httpException(400, """{"detail":"Current password is incorrect"}""")
        val failure = repository.changePassword(1, "Wrong123", "New12345") as ApiResult.Failure
        assertEquals("Current password is incorrect", failure.error.message)
    }
}