package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.EnergyStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Energy figures. The backend calculates and returns these (from SIMULATED readings); this
 * object applies the same documented formulas so the app can explain and verify them, and the
 * unit tests lock the rules down.
 */
object EnergyCalculator {

    /** Average daily kWh of the given recent readings x days in the current month. */
    fun estimateMonthlyKwh(recentKwh: List<Double>, today: LocalDate): Double {
        if (recentKwh.isEmpty()) return 0.0
        val averageDaily = recentKwh.sum() / recentKwh.size
        return round2(averageDaily * today.lengthOfMonth())
    }

    /** monthly fee + estimated kWh x price per kWh, as exact money. */
    fun estimateCost(estimatedKwh: Double, pricePerKwh: BigDecimal, monthlyFee: BigDecimal): BigDecimal =
        (monthlyFee + BigDecimal.valueOf(estimatedKwh).multiply(pricePerKwh))
            .setScale(2, RoundingMode.HALF_UP)

    /** NORMAL below 80% of the plan limit, HIGH from 80% up to 100%, OVER_LIMIT above 100%. */
    fun classifyStatus(estimatedKwh: Double, maxKwh: Int): EnergyStatus {
        val estimated = BigDecimal.valueOf(estimatedKwh)
        val limit = BigDecimal(maxKwh)
        return when {
            estimated < limit.multiply(BigDecimal("0.8")) -> EnergyStatus.NORMAL
            estimated <= limit -> EnergyStatus.HIGH
            else -> EnergyStatus.OVER_LIMIT
        }
    }

    /** Estimated usage as a whole percentage of the plan limit (for a progress indicator). */
    fun percentOfLimit(estimatedKwh: Double, maxKwh: Int): Int =
        if (maxKwh <= 0) 0 else ((estimatedKwh / maxKwh) * 100).roundToInt().coerceAtLeast(0)

    private fun round2(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}