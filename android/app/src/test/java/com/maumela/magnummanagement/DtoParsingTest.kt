package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiJson
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.model.OrderItemRequest
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.SettingsUpdateRequest
import com.maumela.magnummanagement.data.model.Theme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks the DTOs match the exact JSON shapes the FastAPI backend sends and expects. */
class DtoParsingTest {
    private val json = ApiJson.instance

    // Verifies a backend product (snake_case, money as string) decodes with exact decimals.
    @Test
    fun product_decodesBackendJson() {
        val product = json.decodeFromString<ProductDto>(
            """{"id":7,"seller_id":3,"seller_name":"Busi Business","name":"Solar Panel 550W",
            "description":"Panel","price":"1499.00","category":"Solar","sector":"ENERGY",
            "image_url":null,"stock_quantity":10,"availability":true,"rating":4.5,
            "created_at":"2026-09-30T10:00:00Z","updated_at":"2026-09-30T10:00:00Z",
            "some_future_field":"ignored"}""",
        )
        assertEquals(BigDecimal("1499.00"), product.price)
        assertEquals(Sector.ENERGY, product.sector)
        assertEquals(3, product.sellerId)
        assertEquals(null, product.imageUrl)
    }

    // Verifies an order decodes, including per-line statuses and decimal money.
    @Test
    fun order_decodesLinesAndMoney() {
        val order = json.decodeFromString<OrderDto>(
            """{"id":1,"customer_id":2,"status":"PROCESSING","total_amount":"550.50","notes":null,
            "created_at":"2026-09-30T10:00:00Z","updated_at":"2026-09-30T10:00:00Z",
            "items":[{"id":1,"item_type":"SERVICE","product_id":null,"service_id":5,
            "name":"Install","seller_name":"Pieter","quantity":1,"unit_price":"250.50",
            "line_total":"250.50","item_status":"ACCEPTED"}]}""",
        )
        assertEquals(BigDecimal("550.50"), order.totalAmount)
        assertEquals(ItemStatus.ACCEPTED, order.items.single().itemStatus)
        assertEquals(5, order.items.single().serviceId)
    }

    // Verifies money is SENT as a decimal string, never as a floating-point number.
    @Test
    fun productRequest_sendsPriceAsString() {
        val body = json.encodeToString(
            ProductRequest(name = "Panel", price = BigDecimal("10.50"), category = "Solar", sector = Sector.ENERGY),
        )
        assert(""""price":"10.50"""" in body) { body }
        assert(""""sector":"ENERGY"""" in body) { body }
    }

    // Verifies partial updates send only the fields that were set.
    @Test
    fun settingsUpdate_omitsNullFields() {
        assertEquals("""{"theme":"DARK"}""", json.encodeToString(SettingsUpdateRequest(theme = Theme.DARK)))
    }

    // Verifies order lines send ids and quantity only (no prices, no null ids).
    @Test
    fun orderItemRequest_sendsOnlyIdsAndQuantity() {
        assertEquals(
            """{"product_id":1,"quantity":2}""",
            json.encodeToString(OrderItemRequest(productId = 1, quantity = 2)),
        )
    }
}