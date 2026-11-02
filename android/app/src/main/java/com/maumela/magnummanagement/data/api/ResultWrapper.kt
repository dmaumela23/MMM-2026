package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.api.AppError
import kotlin.coroutines.cancellation.CancellationException

/** Every repository call returns this, so a failed request can never crash the app. */
sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

/** Runs [block]; any exception except coroutine cancellation becomes ApiResult.Failure. */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e // never swallow cancellation
} catch (e: Exception) {
    ApiResult.Failure(ApiErrorParser.parse(e))
}