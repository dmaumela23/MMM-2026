package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.utils.EnergyCalculator
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** The expected values are the same ones the backend's own tests use, so both sides agree. */
class EnergyCalculatorTest {

    // Verifies the monthly estimate = average daily kWh x days in the current month (30-day month).
    @Test
    fun monthlyEstimate_usesThirtyDayMonth() {
        assertEquals(450.0, EnergyCalculator.estimateMonthlyKwh(listOf(10.0, 20.0), LocalDate.of(2025, 4, 15)), 0.001)
    }

    // Verifies a leap-year February has 29 days.
    @Test
    fun monthlyEstimate_usesLeapYearFebruary() {
        assertEquals(435.0, EnergyCalculator.estimateMonthlyKwh(listOf(10.0, 20.0), LocalDate.of(2024, 2, 10)), 0.001)
    }

    // Verifies no readings means a zero estimate rather than a crash.
    @Test
    fun monthlyEstimate_emptyHistoryIsZero() {
        assertEquals(0.0, EnergyCalculator.estimateMonthlyKwh(emptyList(), LocalDate.of(2025, 4, 15)), 0.0)
    }

    // Verifies cost = monthly fee + kWh x price per kWh, in exact money.
    @Test
    fun estimatedCost_isFeePlusEnergyCharge() {
        assertEquals(
            BigDecimal("930.00"),
            EnergyCalculator.estimateCost(300.0, BigDecimal("2.60"), BigDecimal("150.00")),
        )
        assertEquals(
            BigDecimal("150.00"),
            EnergyCalculator.estimateCost(0.0, BigDecimal("2.60"), BigDecimal("150.00")),
        )
    }

    // Verifies cost rounding to two decimals.
    @Test
    fun estimatedCost_roundsToTwoDecimals() {
        assertEquals(
            BigDecimal("2.36"),
            EnergyCalculator.estimateCost(1.005, BigDecimal("2.35"), BigDecimal("0.00")),
        )
    }

    // Verifies the NORMAL / HIGH / OVER_LIMIT boundaries for a 1000 kWh plan.
    @Test
    fun status_boundaries() {
        assertEquals(EnergyStatus.NORMAL, EnergyCalculator.classifyStatus(0.0, 1000))
        assertEquals(EnergyStatus.NORMAL, EnergyCalculator.classifyStatus(799.9, 1000))
        assertEquals(EnergyStatus.HIGH, EnergyCalculator.classifyStatus(800.0, 1000))
        assertEquals(EnergyStatus.HIGH, EnergyCalculator.classifyStatus(1000.0, 1000))
        assertEquals(EnergyStatus.OVER_LIMIT, EnergyCalculator.classifyStatus(1000.1, 1000))
    }

    // Verifies the percentage of the plan limit, including a zero limit and values over 100%.
    @Test
    fun percentOfLimit_rules() {
        assertEquals(50, EnergyCalculator.percentOfLimit(350.0, 700))
        assertEquals(120, EnergyCalculator.percentOfLimit(840.0, 700))
        assertEquals(0, EnergyCalculator.percentOfLimit(100.0, 0))
        assertEquals(0, EnergyCalculator.percentOfLimit(0.0, 700))
    }
}