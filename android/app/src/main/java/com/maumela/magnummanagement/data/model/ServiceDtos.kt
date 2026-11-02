@file:UseSerializers(BigDecimalSerializer::class)

package com.maumela.magnummanagement.data.model

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

// There is deliberately no "notes" field: notes belong to the order checkout.

@Serializable
data class ServiceDto(
    val id: Int,
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val category: String,
    val sector: Sector,
    val availability: Boolean,
    val rating: Double,
    @SerialName("delivery_days") val deliveryDays: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ServiceRequest(
    val name: String,
    val description: String = "",
    val price: BigDecimal,
    val category: String,
    val sector: Sector,
    @SerialName("delivery_days") val deliveryDays: Int,
    val availability: Boolean = true,
)

@Serializable
data class ServiceUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val category: String? = null,
    val sector: Sector? = null,
    @SerialName("delivery_days") val deliveryDays: Int? = null,
    val availability: Boolean? = null,
)