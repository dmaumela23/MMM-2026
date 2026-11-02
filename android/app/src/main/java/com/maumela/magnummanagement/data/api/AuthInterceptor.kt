package com.maumela.magnummanagement.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds "Authorization: Bearer <token>" to every request and reports expired sessions.
 *
 * A 401 from /auth/login or /auth/register means "wrong credentials", not "session expired",
 * so those two endpoints are excluded from the expiry signal.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onSessionExpired: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        }

        val response = chain.proceed(request)

        val path = original.url.encodedPath
        val isCredentialEndpoint = path.endsWith("/auth/login") || path.endsWith("/auth/register")
        if (response.code == 401 && !token.isNullOrBlank() && !isCredentialEndpoint) {
            onSessionExpired()
        }
        return response
    }
}