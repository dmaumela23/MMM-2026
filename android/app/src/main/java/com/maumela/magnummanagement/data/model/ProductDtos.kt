@file:UseSerializers(BigDecimalSerializer::class)

package com.maumela.magnummanagement.data.model

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ProductDto(
    val id: Int,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("seller_name") val sellerName: String,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val category: String,
    val sector: Sector,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int,
    val availability: Boolean,
    val rating: Double,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ProductRequest(
    val name: String,
    val description: String = "",
    val price: BigDecimal,
    val category: String,
    val sector: Sector,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    val availability: Boolean = true,
)

/** Partial update: only non-null fields are sent. */
@Serializable
data class ProductUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val category: String? = null,
    val sector: Sector? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    val availability: Boolean? = null,
)