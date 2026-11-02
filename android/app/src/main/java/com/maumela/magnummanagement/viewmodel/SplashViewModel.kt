package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.SessionState
import com.maumela.magnummanagement.utils.ApiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashState {
    data object Checking : SplashState
    data object GoToHome : SplashState
    data object GoToLogin : SplashState
    /** The server could not be reached. The stored login is kept, so Retry can simply try again. */
    data class Error(val message: String) : SplashState
}

/** Shows the MMM branding while the saved login (if any) is checked with the server. */
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val minimumDisplayMillis: Long = 800L,
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Checking)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        check()
    }

    fun check() {
        _state.value = SplashState.Checking
        viewModelScope.launch {
            // Keep the logo on screen briefly, even when the answer comes back instantly.
            val minimum = launch { delay(minimumDisplayMillis) }
            val result = authRepository.restoreSession()
            minimum.join()
            _state.value = when (result) {
                is ApiResult.Success -> when (result.data) {
                    is SessionState.Active -> SplashState.GoToHome
                    SessionState.None -> SplashState.GoToLogin
                }
                is ApiResult.Failure -> SplashState.Error(result.error.message)
            }
        }
    }
}