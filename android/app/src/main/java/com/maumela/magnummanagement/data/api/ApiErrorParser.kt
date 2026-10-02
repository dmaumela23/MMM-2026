package com.maumela.magnummanagement.data.api

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

enum class ErrorKind {
    NO_INTERNET,
    SERVER_UNREACHABLE,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    SERVER_ERROR,
    UNKNOWN,
}

/** A failure the UI can show: [message] is always safe, human-readable text. */
data class AppError(
    val kind: ErrorKind,
    val message: String,
    val httpCode: Int? = null,
)

/** Turns exceptions and HTTP error bodies ({"detail": "..."}) into friendly [AppError]s. */
object ApiErrorParser {
    const val NO_INTERNET_MESSAGE = "No internet connection. Check your network and try again."
    const val SERVER_UNREACHABLE_MESSAGE =
        "Can't reach the MMM server. Make sure it is running and try again."
    const val SERVER_ERROR_MESSAGE = "Something went wrong on our side. Please try again later."
    const val UNEXPECTED_RESPONSE_MESSAGE = "The server sent an unexpected response."
    const val UNKNOWN_MESSAGE = "Something went wrong. Please try again."

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(throwable: Throwable): AppError = when (throwable) {
        is HttpException -> fromHttp(throwable.code(), readBody(throwable))
        is UnknownHostException -> AppError(ErrorKind.NO_INTERNET, NO_INTERNET_MESSAGE)
        is ConnectException, is SocketTimeoutException ->
            AppError(ErrorKind.SERVER_UNREACHABLE, SERVER_UNREACHABLE_MESSAGE)
        is IOException -> AppError(ErrorKind.NO_INTERNET, NO_INTERNET_MESSAGE)
        is SerializationException -> AppError(ErrorKind.UNKNOWN, UNEXPECTED_RESPONSE_MESSAGE)
        else -> AppError(ErrorKind.UNKNOWN, UNKNOWN_MESSAGE)
    }

    /** Pure function (no Android or network): maps a status code and response body to an error. */
    fun fromHttp(code: Int, body: String?): AppError {
        val detail = extractDetail(body)
        return when (code) {
            400 -> AppError(ErrorKind.BAD_REQUEST, detail ?: "The request was not valid.", code)
            401 -> AppError(ErrorKind.UNAUTHORIZED, detail ?: "Please log in again.", code)
            403 -> AppError(ErrorKind.FORBIDDEN, detail ?: "You don't have permission to do this.", code)
            404 -> AppError(ErrorKind.NOT_FOUND, detail ?: "We couldn't find what you were looking for.", code)
            409 -> AppError(ErrorKind.CONFLICT, detail ?: "That conflicts with existing data.", code)
            // Never show server internals for 5xx, whatever the body says.
            in 500..599 -> AppError(ErrorKind.SERVER_ERROR, SERVER_ERROR_MESSAGE, code)
            else -> AppError(ErrorKind.UNKNOWN, detail ?: "Unexpected error (HTTP $code).", code)
        }
    }

    private fun readBody(exception: HttpException): String? = try {
        exception.response()?.errorBody()?.string()
    } catch (_: IOException) {
        null
    }

    private fun extractDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            json.parseToJsonElement(body).jsonObject["detail"]
                ?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null // not JSON, or "detail" is not a plain string
        }
    }
}