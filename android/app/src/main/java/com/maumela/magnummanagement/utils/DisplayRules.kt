package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderStatus

/** Meaning of a status colour. The UI always shows a text label too, never colour alone. */
enum class Tone { POSITIVE, WARNING, NEGATIVE, INFO, NEUTRAL }

data class Availability(val label: String, val canOrder: Boolean, val tone: Tone)

/** Small display decisions kept out of the composables so they can be unit tested. */
object DisplayRules {
    const val LOW_STOCK_THRESHOLD = 5

    fun tone(status: ItemStatus): Tone = when (status) {
        ItemStatus.PENDING -> Tone.WARNING
        ItemStatus.ACCEPTED -> Tone.INFO
        ItemStatus.IN_PROGRESS -> Tone.INFO
        ItemStatus.COMPLETED -> Tone.POSITIVE
        ItemStatus.CANCELLED -> Tone.NEGATIVE
    }

    fun tone(status: OrderStatus): Tone = when (status) {
        OrderStatus.PENDING -> Tone.WARNING
        OrderStatus.PROCESSING -> Tone.INFO
        OrderStatus.COMPLETED -> Tone.POSITIVE
        OrderStatus.CANCELLED -> Tone.NEGATIVE
    }

    fun tone(status: EnergyStatus): Tone = when (status) {
        EnergyStatus.NORMAL -> Tone.POSITIVE
        EnergyStatus.HIGH -> Tone.WARNING
        EnergyStatus.OVER_LIMIT -> Tone.NEGATIVE
    }

    /** Same rule as the server: orderable only when the flag is on AND stock is above zero. */
    fun productAvailability(availability: Boolean, stock: Int): Availability = when {
        !availability -> Availability("Unavailable", false, Tone.NEGATIVE)
        stock <= 0 -> Availability("Out of stock", false, Tone.NEGATIVE)
        stock <= LOW_STOCK_THRESHOLD -> Availability("Only $stock left", true, Tone.WARNING)
        else -> Availability("In stock", true, Tone.POSITIVE)
    }

    fun serviceAvailability(availability: Boolean): Availability =
        if (availability) Availability("Available", true, Tone.POSITIVE)
        else Availability("Unavailable", false, Tone.NEGATIVE)

    /** Whole stars to fill (the exact rating is also shown as text). 4.4 -> 4, 4.5 -> 5. */
    fun starsFilled(rating: Double): Int = (rating.coerceIn(0.0, 5.0) + 0.5).toInt().coerceAtMost(5)

    fun deliveryLabel(days: Int): String =
        "Estimated delivery: $days ${if (days == 1) "day" else "days"}"

    /** Text read aloud by screen readers in place of a chart. */
    fun kwhSummary(values: List<Double>): String {
        if (values.isEmpty()) return "No usage data"
        val count = if (values.size == 1) "1 reading" else "${values.size} readings"
        return "$count: lowest ${Formatters.kwh(values.min())}, " +
                "highest ${Formatters.kwh(values.max())}, average ${Formatters.kwh(values.average())}"
    }
}