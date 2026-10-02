package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.local.TokenStore
import com.maumela.magnummanagement.data.model.AuthResponse
import com.maumela.magnummanagement.data.model.LoginRequest
import com.maumela.magnummanagement.data.model.PasswordChangeRequest
import com.maumela.magnummanagement.data.model.RegisterRequest
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserUpdateRequest
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.map
import com.maumela.magnummanagement.utils.safeApiCall
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    data class Active(val user: UserDto) : SessionState
    data object None : SessionState
}

/**
 * Registration, login, session restore, logout, and the user's own profile.
 *
 * @param clearLocalData wipes the Room cache (called on logout). Passed in so tests need no database.
 */
class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val preferencesStore: PreferencesStore,
    private val clearLocalData: suspend () -> Unit,
) {
    private val _currentUser = MutableStateFlow<UserDto?>(null)

    /** The logged-in user (role drives which buttons the UI shows). Null when logged out. */
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    suspend fun register(request: RegisterRequest): ApiResult<UserDto> =
        authenticate { api.register(request) }

    suspend fun login(email: String, password: String): ApiResult<UserDto> =
        authenticate { api.login(LoginRequest(email.trim(), password)) }

    private suspend fun authenticate(call: suspend () -> AuthResponse): ApiResult<UserDto> {
        val result = safeApiCall(call)
        if (result is ApiResult.Failure) return result
        val response = (result as ApiResult.Success).data
        try {
            tokenStore.save(response.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return ApiResult.Failure(
                AppError(ErrorKind.UNKNOWN, "Couldn't store your login securely on this device."),
            )
        }
        _currentUser.value = response.user
        return ApiResult.Success(response.user)
    }

    /**
     * Called by the Splash screen.
     *  - no stored token            -> Success(None)   (go to Login)
     *  - token accepted             -> Success(Active) (go to Home)
     *  - token rejected (401)       -> Success(None)   (token cleared, go to Login)
     *  - network/server problem     -> Failure         (Splash offers Retry; the token is kept)
     */
    suspend fun restoreSession(): ApiResult<SessionState> {
        tokenStore.load() ?: return ApiResult.Success(SessionState.None)
        return when (val result = safeApiCall { api.me() }) {
            is ApiResult.Success -> {
                _currentUser.value = result.data
                ApiResult.Success(SessionState.Active(result.data))
            }
            is ApiResult.Failure -> {
                if (result.error.kind == ErrorKind.UNAUTHORIZED) {
                    logout()
                    ApiResult.Success(SessionState.None)
                } else {
                    result
                }
            }
        }
    }

    /** Removes the token, the saved preferences and every cached row. */
    suspend fun logout() {
        tokenStore.clear()
        preferencesStore.clear()
        try {
            clearLocalData()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The cache is rebuilt from the server anyway.
        }
        _currentUser.value = null
    }

    suspend fun updateProfile(
        userId: Int,
        fullName: String? = null,
        email: String? = null,
        phone: String? = null,
    ): ApiResult<UserDto> {
        val body = UserUpdateRequest(fullName?.trim(), email?.trim(), phone?.trim())
        val result = safeApiCall { api.updateUser(userId, body) }
        if (result is ApiResult.Success) _currentUser.value = result.data
        return result
    }

    suspend fun changePassword(userId: Int, currentPassword: String, newPassword: String): ApiResult<Unit> =
        safeApiCall { api.changePassword(userId, PasswordChangeRequest(currentPassword, newPassword)) }
            .map { }
}