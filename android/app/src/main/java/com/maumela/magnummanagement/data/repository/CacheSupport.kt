package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.utils.ApiResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Data returned to the UI. [isStale] is true when the network failed and the data comes from
 * the LOCAL CACHE (the UI shows a "Showing saved data" banner). The server is the source of truth.
 */
data class Cached<T>(val data: T, val isStale: Boolean)

/** Only connection problems justify showing cached data. A 401/403/404/5xx is a real answer. */
fun ErrorKind.isConnectionProblem(): Boolean =
    this == ErrorKind.NO_INTERNET || this == ErrorKind.SERVER_UNREACHABLE

/** A cache write that fails (disk full, etc.) must never turn a successful API call into an error. */
suspend fun cacheWrite(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // The cache is optional; ignore.
    }
}

/** Returns null when the cache cannot be read. */
suspend fun <T> cacheRead(block: suspend () -> T?): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/**
 * Read-through pattern used by every cached resource:
 * 1. ask the server (source of truth) and refresh the cache with the answer;
 * 2. only if the NETWORK is the problem, fall back to the cache;
 * 3. otherwise report the real error.
 */
suspend fun <T> fetchWithCache(
    allowFallback: Boolean = true,
    fetch: suspend () -> T,
    save: suspend (T) -> Unit,
    readCache: suspend () -> T?,
): ApiResult<Cached<T>> {
    try {
        val fresh = fetch()
        cacheWrite { save(fresh) }
        return ApiResult.Success(Cached(fresh, isStale = false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = ApiErrorParser.parse(e)
        if (allowFallback && error.kind.isConnectionProblem()) {
            val cached = cacheRead(readCache)
            if (cached != null) return ApiResult.Success(Cached(cached, isStale = true))
        }
        return ApiResult.Failure(error)
    }
}