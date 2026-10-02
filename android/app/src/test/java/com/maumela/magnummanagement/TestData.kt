package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.ItemType
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.model.OrderItemDto
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserRole
import java.math.BigDecimal
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Test builders shared by the repository tests. */

fun httpException(code: Int, body: String): HttpException =
    HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

fun testProduct(
    id: Int = 1,
    name: String = "Solar Panel",
    description: String = "",
    price: String = "1499.00",
    category: String = "Solar",
    sector: Sector = Sector.ENERGY,
    stock: Int = 10,
    availability: Boolean = true,
    rating: Double = 4.5,
) = ProductDto(
    id = id, sellerId = 3, sellerName = "Busi Business", name = name, description = description,
    price = BigDecimal(price), category = category, sector = sector, imageUrl = null,
    stockQuantity = stock, availability = availability, rating = rating,
    createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
)

fun testService(
    id: Int = 1,
    name: String = "Installation",
    description: String = "",
    price: String = "250.50",
    category: String = "Installation",
    sector: Sector = Sector.SERVICE,
    availability: Boolean = true,
    rating: Double = 4.0,
) = ServiceDto(
    id = id, providerId = 4, providerName = "Pieter Provider", name = name, description = description,
    price = BigDecimal(price), category = category, sector = sector, availability = availability,
    rating = rating, deliveryDays = 5,
    createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
)

fun testUser(id: Int = 1, role: UserRole = UserRole.CUSTOMER, fullName: String = "Cathy Customer") = UserDto(
    id = id, fullName = fullName, email = "cathy@example.com", phone = null, role = role,
    createdAt = "2026-09-30T10:00:00Z",
)

fun testOrder(id: Int = 1, total: String = "550.50") = OrderDto(
    id = id, customerId = 1, status = OrderStatus.PENDING, totalAmount = BigDecimal(total),
    createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
    items = listOf(
        OrderItemDto(
            id = 1, itemType = ItemType.SERVICE, serviceId = 5, name = "Install", sellerName = "Pieter",
            quantity = 1, unitPrice = BigDecimal("250.50"), lineTotal = BigDecimal("250.50"),
            itemStatus = ItemStatus.PENDING,
        ),
    ),
)

fun testOrderSummary(id: Int = 1) = OrderSummaryDto(
    id = id, status = OrderStatus.PENDING, totalAmount = BigDecimal("550.50"),
    createdAt = "2026-09-30T10:00:00Z", itemCount = 2, firstItemName = "Battery",
)