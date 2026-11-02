package com.maumela.magnummanagement.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * All display formatting lives here, so it is identical on every screen and unit-testable.
 * Money is South African rand: "R1 234.50". The format never depends on the phone's locale.
 */
object Formatters {
    private val DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    fun money(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        val parts = scaled.abs().toPlainString().split(".")
        val grouped = parts[0].reversed().chunked(3).joinToString(" ").reversed()
        val sign = if (scaled.signum() < 0) "-" else ""
        return "${sign}R$grouped.${parts[1]}"
    }

    /** Parses a decimal string such as "1499.00" from the API. Returns null if it isn't a number. */
    fun parseMoney(text: String?): BigDecimal? = text?.trim()?.toBigDecimalOrNull()

    fun kwh(value: Double): String = String.format(Locale.US, "%.1f kWh", value)

    /** Server timestamps are UTC ("2026-09-30T10:00:00Z"); they are shown in the device's time zone. */
    fun dateTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(zone).format(DATE_TIME)
    } catch (_: Exception) {
        iso
    }

    fun date(iso: String, zone: ZoneId = ZoneId.systemDefault()): String = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(zone).format(DATE)
    } catch (_: Exception) {
        iso
    }

    /** A plain calendar date such as "2026-09-30" (energy readings), shown as "30 Sep". */
    fun shortDate(isoDate: String): String = try {
        LocalDate.parse(isoDate).format(SHORT_DATE)
    } catch (_: Exception) {
        isoDate
    }

    /** "2026-09" -> "Sep 2026" */
    fun month(yearMonth: String): String = try {
        YearMonth.parse(yearMonth).format(MONTH)
    } catch (_: Exception) {
        yearMonth
    }

    /** "IN_PROGRESS" -> "In progress" */
    fun enumLabel(name: String): String =
        name.lowercase(Locale.ENGLISH).replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun roleLabel(role: String): String = when (role) {
        "CUSTOMER" -> "Customer"
        "PROVIDER" -> "Service Provider"
        "BUSINESS" -> "Business"
        else -> enumLabel(role)
    }

    /** Avatar initials: first letters of the first two words ("Cathy Customer" -> "CC"). */
    fun initials(fullName: String): String {
        val letters = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).map { it.first().uppercaseChar() }
        return if (letters.isEmpty()) "?" else letters.joinToString("")
    }
}