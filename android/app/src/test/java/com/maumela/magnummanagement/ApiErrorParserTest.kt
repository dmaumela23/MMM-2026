package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.safeApiCall
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiErrorParserTest {

    private fun httpException(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    // Verifies a 400 shows the server's own validation message.
    @Test
    fun badRequest_usesServerDetail() {
        val error = ApiErrorParser.fromHttp(400, """{"detail":"Password must contain at least one digit"}""")
        assertEquals(ErrorKind.BAD_REQUEST, error.kind)
        assertEquals("Password must contain at least one digit", error.message)
        assertEquals(400, error.httpCode)
    }

    // Verifies a failed login shows the server's message, not a session-expired message.
    @Test
    fun unauthorized_usesServerDetail() {
        val error = ApiErrorParser.fromHttp(401, """{"detail":"Incorrect email or password"}""")
        assertEquals(ErrorKind.UNAUTHORIZED, error.kind)
        assertEquals("Incorrect email or password", error.message)
    }

    // Verifies 403, 404 and 409 map to their kinds.
    @Test
    fun forbiddenNotFoundConflict_mapToKinds() {
        assertEquals(ErrorKind.FORBIDDEN, ApiErrorParser.fromHttp(403, """{"detail":"No"}""").kind)
        assertEquals(ErrorKind.NOT_FOUND, ApiErrorParser.fromHttp(404, """{"detail":"Gone"}""").kind)
        assertEquals(ErrorKind.CONFLICT, ApiErrorParser.fromHttp(409, """{"detail":"Taken"}""").kind)
    }

    // Verifies missing bodies fall back to friendly default text.
    @Test
    fun missingBody_usesDefaultMessage() {
        val error = ApiErrorParser.fromHttp(404, null)
        assertEquals(ErrorKind.NOT_FOUND, error.kind)
        assertTrue(error.message.isNotBlank())
    }

    // Verifies a malformed body does not crash and falls back to the default text.
    @Test
    fun malformedBody_usesDefaultMessage() {
        val error = ApiErrorParser.fromHttp(400, "{not json")
        assertEquals(ErrorKind.BAD_REQUEST, error.kind)
        assertEquals("The request was not valid.", error.message)
    }

    // Verifies 5xx never leaks server internals, even if the body contains them.
    @Test
    fun serverError_isGenericAndHidesBody() {
        val error = ApiErrorParser.fromHttp(500, """{"detail":"psycopg2.OperationalError host=10.0.0.5"}""")
        assertEquals(ErrorKind.SERVER_ERROR, error.kind)
        assertEquals(ApiErrorParser.SERVER_ERROR_MESSAGE, error.message)
        assertFalse(error.message.contains("psycopg2"))
    }

    // Verifies a real HttpException is read through its error body.
    @Test
    fun httpException_isParsedFromItsBody() {
        val error = ApiErrorParser.parse(httpException(409, """{"detail":"An account with this email already exists"}"""))
        assertEquals(ErrorKind.CONFLICT, error.kind)
        assertEquals("An account with this email already exists", error.message)
    }

    // Verifies network failures map to "no internet" or "server unreachable".
    @Test
    fun networkExceptions_mapToFriendlyKinds() {
        assertEquals(ErrorKind.NO_INTERNET, ApiErrorParser.parse(UnknownHostException()).kind)
        assertEquals(ErrorKind.SERVER_UNREACHABLE, ApiErrorParser.parse(ConnectException()).kind)
        assertEquals(ErrorKind.SERVER_UNREACHABLE, ApiErrorParser.parse(SocketTimeoutException()).kind)
        assertEquals(ErrorKind.NO_INTERNET, ApiErrorParser.parse(java.io.IOException("boom")).kind)
    }

    // Verifies a JSON decoding problem and unknown exceptions do not crash.
    @Test
    fun otherExceptions_areUnknown() {
        assertEquals(ErrorKind.UNKNOWN, ApiErrorParser.parse(SerializationException("bad json")).kind)
        assertEquals(ErrorKind.UNKNOWN, ApiErrorParser.parse(IllegalStateException("x")).kind)
    }

    // Verifies safeApiCall wraps success and failure instead of throwing.
    @Test
    fun safeApiCall_wrapsSuccessAndFailure() = runTest {
        val ok = safeApiCall { 42 }
        assertEquals(ApiResult.Success(42), ok)

        val failed = safeApiCall<Int> { throw UnknownHostException() }
        assertTrue(failed is ApiResult.Failure)
        assertEquals(ErrorKind.NO_INTERNET, (failed as ApiResult.Failure).error.kind)
    }

    // Verifies coroutine cancellation is rethrown, not swallowed as an error.
    @Test
    fun safeApiCall_rethrowsCancellation() = runTest {
        try {
            safeApiCall<Int> { throw CancellationException("cancelled") }
            fail("CancellationException should have been rethrown")
        } catch (expected: CancellationException) {
            // correct
        }
    }
}