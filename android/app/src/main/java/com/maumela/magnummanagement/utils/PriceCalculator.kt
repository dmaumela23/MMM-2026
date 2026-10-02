package com.maumela.magnummanagement.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Totals shown in the order draft. They are a PREVIEW only: when the order is submitted the
 * server loads current prices and calculates the real total. All money is BigDecimal.
 */
object PriceCalculator {
    val ZERO: BigDecimal = BigDecimal("0.00")
    const val MAX_PRODUCT_QUANTITY = 99

    fun lineTotal(unitPrice: BigDecimal, quantity: Int): BigDecimal {
        require(quantity >= 0) { "quantity cannot be negative" }
        return unitPrice.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    /** Each line is (unit price, quantity). */
    fun orderTotal(lines: List<Pair<BigDecimal, Int>>): BigDecimal =
        lines.fold(ZERO) { total, (price, quantity) -> total + lineTotal(price, quantity) }
            .setScale(2, RoundingMode.HALF_UP)

    /** Services are always quantity 1; products are limited to 1..99 (same as the server). */
    fun clampQuantity(quantity: Int, isService: Boolean): Int =
        if (isService) 1 else quantity.coerceIn(1, MAX_PRODUCT_QUANTITY)
}