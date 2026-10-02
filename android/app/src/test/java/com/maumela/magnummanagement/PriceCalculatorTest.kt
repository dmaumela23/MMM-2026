package com.maumela.magnummanagement

import com.maumela.magnummanagement.utils.PriceCalculator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceCalculatorTest {

    // Verifies a line total is exact decimal arithmetic (price x quantity).
    @Test
    fun lineTotal_multipliesExactly() {
        assertEquals(BigDecimal("59.97"), PriceCalculator.lineTotal(BigDecimal("19.99"), 3))
        assertEquals(BigDecimal("0.00"), PriceCalculator.lineTotal(BigDecimal("19.99"), 0))
    }

    // Verifies half-up rounding to two places.
    @Test
    fun lineTotal_roundsHalfUp() {
        assertEquals(BigDecimal("0.01"), PriceCalculator.lineTotal(BigDecimal("0.005"), 1))
        assertEquals(BigDecimal("0.01"), PriceCalculator.lineTotal(BigDecimal("0.004"), 2))
    }

    // Verifies the order total uses exact decimals (doubles would give 0.5000000000000001).
    @Test
    fun orderTotal_isExact() {
        val lines = listOf(BigDecimal("0.10") to 3, BigDecimal("0.20") to 1)
        assertEquals(BigDecimal("0.50"), PriceCalculator.orderTotal(lines))
    }

    // Verifies the total of a mixed order (products with quantities and a service).
    @Test
    fun orderTotal_mixedOrder() {
        val lines = listOf(BigDecimal("100.00") to 3, BigDecimal("250.50") to 1)
        assertEquals(BigDecimal("550.50"), PriceCalculator.orderTotal(lines))
    }

    // Verifies an empty draft totals R0.00 (and keeps two decimals).
    @Test
    fun orderTotal_emptyIsZero() {
        assertEquals(BigDecimal("0.00"), PriceCalculator.orderTotal(emptyList()))
    }

    // Verifies large values do not lose precision.
    @Test
    fun orderTotal_largeValues() {
        val lines = listOf(BigDecimal("14999.00") to 99, BigDecimal("12499.00") to 99)
        assertEquals(BigDecimal("2722302.00"), PriceCalculator.orderTotal(lines))
    }

    // Verifies a negative quantity is refused.
    @Test(expected = IllegalArgumentException::class)
    fun lineTotal_negativeQuantityIsRefused() {
        PriceCalculator.lineTotal(BigDecimal("10.00"), -1)
    }

    // Verifies quantities are clamped: services always 1, products 1 to 99.
    @Test
    fun clampQuantity_rules() {
        assertEquals(1, PriceCalculator.clampQuantity(5, isService = true))
        assertEquals(1, PriceCalculator.clampQuantity(0, isService = false))
        assertEquals(1, PriceCalculator.clampQuantity(-3, isService = false))
        assertEquals(7, PriceCalculator.clampQuantity(7, isService = false))
        assertEquals(99, PriceCalculator.clampQuantity(500, isService = false))
    }
}