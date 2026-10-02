package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.api.AppError

/**
 * What a screen shows. ViewModels expose this as a StateFlow, so a failed request becomes
 * a state ("Failure") and never a crash. [Success.isStale] means the data came from the local
 * cache because the network failed (the UI shows a "Showing saved data" banner).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T, val isStale: Boolean = false) : UiState<T>
    data class Failure(val error: AppError) : UiState<Nothing>
}