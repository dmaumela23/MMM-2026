@file:UseSerializers(BigDecimalSerializer::class)

package com.maumela.magnummanagement.data.model

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

// ALL energy data is SIMULATED. Every response carries isSimulated = true and a disclaimer.

@Serializable
data class EnergyPlanDto(
    val id: Int,
    val name: String,
    val description: String,
    @SerialName("price_per_kwh") val pricePerKwh: BigDecimal,
    @SerialName("monthly_fee") val monthlyFee: BigDecimal,
    @SerialName("max_kwh") val maxKwh: Int,
)

@Serializable
data class UsagePointDto(
    val date: String, // "YYYY-MM-DD"
    val kwh: Double,
    @SerialName("estimated_cost") val estimatedCost: BigDecimal,
    @SerialName("is_simulated") val isSimulated: Boolean,
)

@Serializable
data class EnergyUsageResponse(
    val days: Int,
    @SerialName("current_kwh") val currentKwh: Double,
    @SerialName("estimated_monthly_kwh") val estimatedMonthlyKwh: Double,
    @SerialName("estimated_cost") val estimatedCost: BigDecimal,
    val status: EnergyStatus,
    val plan: EnergyPlanDto,
    val history: List<UsagePointDto>,
    @SerialName("is_simulated") val isSimulated: Boolean,
    val disclaimer: String,
)

@Serializable
data class EnergyPlansResponse(
    @SerialName("current_plan_id") val currentPlanId: Int? = null,
    val plans: List<EnergyPlanDto>,
    @SerialName("is_simulated") val isSimulated: Boolean,
    val disclaimer: String,
)

@Serializable
data class MonthlyReportDto(
    val month: String, // "YYYY-MM"
    @SerialName("total_kwh") val totalKwh: Double,
    @SerialName("average_daily_kwh") val averageDailyKwh: Double,
    @SerialName("days_recorded") val daysRecorded: Int,
    @SerialName("estimated_cost") val estimatedCost: BigDecimal,
)

@Serializable
data class EnergyReportsResponse(
    val months: List<MonthlyReportDto>,
    @SerialName("is_simulated") val isSimulated: Boolean,
    val disclaimer: String,
)