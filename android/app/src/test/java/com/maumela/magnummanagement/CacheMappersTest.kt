package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.local.toDto
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheMappersTest {
    private val product = ProductDto(
        id = 7, sellerId = 3, sellerName = "Busi", name = "Solar Panel", description = "550W",
        price = BigDecimal("1499.00"), category = "Solar", sector = Sector.ENERGY,
        imageUrl = "https://example.com/p.png", stockQuantity = 10, availability = true,
        rating = 4.5, createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T11:00:00Z",
    )

    // Verifies a product survives cache storage unchanged.
    @Test
    fun product_roundTripsThroughEntity() {
        assertEquals(product, product.toEntity().toDto())
    }

    // Verifies a missing image URL stays null and money keeps its two decimals.
    @Test
    fun product_keepsNullImageAndDecimalScale() {
        val original = product.copy(imageUrl = null, price = BigDecimal("10.50"))
        val entity = original.toEntity()
        assertEquals("10.50", entity.price)
        assertEquals(original, entity.toDto())
    }

    // Verifies a service survives cache storage unchanged.
    @Test
    fun service_roundTripsThroughEntity() {
        val service = ServiceDto(
            id = 2, providerId = 4, providerName = "Pieter", name = "Install", description = "Roof work",
            price = BigDecimal("250.50"), category = "Installation", sector = Sector.SERVICE,
            availability = true, rating = 4.0, deliveryDays = 5,
            createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
        )
        assertEquals(service, service.toEntity().toDto())
    }

    // Verifies an order summary survives cache storage unchanged.
    @Test
    fun orderSummary_roundTripsThroughEntity() {
        val order = OrderSummaryDto(
            id = 9, status = OrderStatus.PROCESSING, totalAmount = BigDecimal("550.50"),
            createdAt = "2026-09-30T10:00:00Z", itemCount = 2, firstItemName = "Battery",
        )
        assertEquals(order, order.toEntity().toDto())
    }
}