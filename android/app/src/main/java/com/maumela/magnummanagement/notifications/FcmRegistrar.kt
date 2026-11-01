package com.maumela.magnummanagement.notifications

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps the Firebase Cloud Messaging SDK (the project's required SDK).
 *
 * Firebase needs the google-services.json file from YOUR Firebase project. Without it the app still
 * works: [isAvailable] is false and every call here safely returns null/false instead of crashing.
 */
class FcmRegistrar(@Suppress("unused") private val context: Context) {

    private fun messaging(): FirebaseMessaging? = try {
        FirebaseMessaging.getInstance() // throws when Firebase is not initialised (no google-services.json)
    } catch (_: Exception) {
        null
    }

    fun isAvailable(): Boolean = messaging() != null

    /** The device's FCM token, or null if Firebase is not configured or the token could not be fetched. */
    suspend fun fetchToken(): String? {
        val instance = messaging() ?: return null
        return try {
            instance.token.awaitOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /** Deletes the device token (used when notifications are switched off). */
    suspend fun deleteToken(): Boolean {
        val instance = messaging() ?: return false
        return try {
            instance.deleteToken().awaitCompletion()
        } catch (_: Exception) {
            false
        }
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine<T?> { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) continuation.resume(if (task.isSuccessful) task.result else null)
    }
}

private suspend fun Task<*>.awaitCompletion(): Boolean = suspendCancellableCoroutine<Boolean> { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) continuation.resume(task.isSuccessful)
    }
}
