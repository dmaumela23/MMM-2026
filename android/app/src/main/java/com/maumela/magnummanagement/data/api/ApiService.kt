package com.maumela.magnummanagement.data.api

import com.maumela.magnummanagement.data.model.AuthResponse
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyReportsResponse
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.data.model.IncomingItemDto
import com.maumela.magnummanagement.data.model.ItemStatusUpdateRequest
import com.maumela.magnummanagement.data.model.LoginRequest
import com.maumela.magnummanagement.data.model.MessageResponse
import com.maumela.magnummanagement.data.model.OrderCancelRequest
import com.maumela.magnummanagement.data.model.OrderCreateRequest
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.model.OrderItemDto
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.data.model.PasswordChangeRequest
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.ProductUpdateRequest
import com.maumela.magnummanagement.data.model.RegisterRequest
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.data.model.ServiceUpdateRequest
import com.maumela.magnummanagement.data.model.SettingsDto
import com.maumela.magnummanagement.data.model.SettingsUpdateRequest
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The MMM REST API. Paths are relative to the base URL (which ends in /api/).
 * A non-2xx response makes the suspend function throw retrofit2.HttpException;
 * repositories convert that (and network errors) into ApiResult.Failure.
 */
interface ApiService {

    // ---- Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): UserDto

    // ---- Users (own profile only)
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserDto

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body body: UserUpdateRequest): UserDto

    @PUT("users/{id}/password")
    suspend fun changePassword(@Path("id") id: Int, @Body body: PasswordChangeRequest): MessageResponse

    // ---- Settings
    @GET("settings")
    suspend fun getSettings(): SettingsDto

    @PUT("settings")
    suspend fun updateSettings(@Body body: SettingsUpdateRequest): SettingsDto

    // ---- Products (filters are applied by the SERVER as SQL conditions)
    @GET("products")
    suspend fun getProducts(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("sector") sector: String? = null,
        @Query("min_price") minPrice: String? = null,
        @Query("max_price") maxPrice: String? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("availability") availability: Boolean? = null,
        @Query("owner") owner: String? = null, // "me" = only my products
    ): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto

    @POST("products")
    suspend fun createProduct(@Body body: ProductRequest): ProductDto

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body body: ProductUpdateRequest): ProductDto

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): MessageResponse

    // ---- Services
    @GET("services")
    suspend fun getServices(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("sector") sector: String? = null,
        @Query("min_price") minPrice: String? = null,
        @Query("max_price") maxPrice: String? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("availability") availability: Boolean? = null,
        @Query("owner") owner: String? = null,
    ): List<ServiceDto>

    @GET("services/{id}")
    suspend fun getService(@Path("id") id: Int): ServiceDto

    @POST("services")
    suspend fun createService(@Body body: ServiceRequest): ServiceDto

    @PUT("services/{id}")
    suspend fun updateService(@Path("id") id: Int, @Body body: ServiceUpdateRequest): ServiceDto

    @DELETE("services/{id}")
    suspend fun deleteService(@Path("id") id: Int): MessageResponse

    // ---- Orders
    @POST("orders")
    suspend fun createOrder(@Body body: OrderCreateRequest): OrderDto

    @GET("orders")
    suspend fun getOrders(): List<OrderSummaryDto>

    @GET("orders/incoming")
    suspend fun getIncomingItems(): List<IncomingItemDto>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): OrderDto

    @PUT("orders/{id}")
    suspend fun cancelOrder(@Path("id") id: Int, @Body body: OrderCancelRequest): OrderDto

    @PUT("orders/{orderId}/items/{itemId}/status")
    suspend fun updateItemStatus(
        @Path("orderId") orderId: Int,
        @Path("itemId") itemId: Int,
        @Body body: ItemStatusUpdateRequest,
    ): OrderItemDto

    // ---- Energy (SIMULATED data)
    @GET("energy/usage")
    suspend fun getEnergyUsage(@Query("days") days: Int = 30): EnergyUsageResponse

    @GET("energy/plans")
    suspend fun getEnergyPlans(): EnergyPlansResponse

    @GET("energy/reports")
    suspend fun getEnergyReports(): EnergyReportsResponse
}