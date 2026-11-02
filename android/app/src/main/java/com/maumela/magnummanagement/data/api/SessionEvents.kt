package com.maumela.magnummanagement.data.api

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Broadcasts "the session has expired" (a 401 on a normal request).
 * The navigation layer listens and returns the user to the Login screen.
 */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    /** Safe to call from any thread (OkHttp's worker threads call this). */
    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}