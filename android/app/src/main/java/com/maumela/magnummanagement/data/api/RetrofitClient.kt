package com.maumela.magnummanagement.data.api

//import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

import com.maumela.magnummanagement.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the single ApiService used by the whole app.
 *
 * @param baseUrl must end with "/" (BuildConfig.MMM_BASE_URL already does)
 * @param tokenProvider returns the current JWT, or null when logged out
 */
class RetrofitClient(
    baseUrl: String,
    tokenProvider: () -> String?,
    sessionEvents: SessionEvents,
    debug: Boolean = BuildConfig.DEBUG,
) {
    val api: ApiService

    init {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider) { sessionEvents.notifyExpired() })
            .apply {
                if (debug) {
                    // BASIC = method, URL, status. Never BODY: bodies contain passwords and tokens.
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        },
                    )
                }
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(ApiJson.instance.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}