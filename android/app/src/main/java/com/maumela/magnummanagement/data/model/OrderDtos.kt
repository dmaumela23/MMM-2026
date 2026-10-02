@file:UseSerializers(BigDecimalSerializer::class)

package com.maumela.magnummanagement.data.model

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** The client sends ids and quantities ONLY. Prices and totals are always calculated by the server. */
@Serializable
data class OrderItemRequest(
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("service_id") val serviceId: Int? = null,
    val quantity: Int = 1,
)

@Serializable
data class OrderCreateRequest(
    val items: List<OrderItemRequest>,
    val notes: String? = null,
)

@Serializable
data class OrderCancelRequest(val status: OrderStatus = OrderStatus.CANCELLED)

@Serializable
data class ItemStatusUpdateRequest(@SerialName("item_status") val itemStatus: ItemStatus)

@Serializable
data class OrderItemDto(
    val id: Int,
    @SerialName("item_type") val itemType: ItemType,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("service_id") val serviceId: Int? = null,
    val name: String,
    @SerialName("seller_name") val sellerName: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: BigDecimal,
    @SerialName("line_total") val lineTotal: BigDecimal,
    @SerialName("item_status") val itemStatus: ItemStatus,
)

@Serializable
data class OrderDto(
    val id: Int,
    @SerialName("customer_id") val customerId: Int,
    val status: OrderStatus,
    @SerialName("total_amount") val totalAmount: BigDecimal,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val items: List<OrderItemDto>,
)

@Serializable
data class OrderSummaryDto(
    val id: Int,
    val status: OrderStatus,
    @SerialName("total_amount") val totalAmount: BigDecimal,
    @SerialName("created_at") val createdAt: String,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("first_item_name") val firstItemName: String? = null,
)

@Serializable
data class IncomingItemDto(
    @SerialName("order_id") val orderId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_type") val itemType: ItemType,
    val name: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: BigDecimal,
    @SerialName("line_total") val lineTotal: BigDecimal,
    @SerialName("item_status") val itemStatus: ItemStatus,
    @SerialName("customer_name") val customerName: String,
    val notes: String? = null,
    @SerialName("order_created_at") val orderCreatedAt: String,
)