package com.maumela.magnummanagement

import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.SectorRegistry
import com.maumela.magnummanagement.data.model.Sector
import java.math.BigDecimal
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    private val johannesburg = ZoneId.of("Africa/Johannesburg") // UTC+2

    // Verifies rand formatting: "R" prefix, space as thousands separator, two decimals.
    @Test
    fun money_formatsAsRand() {
        assertEquals("R1 234.50", Formatters.money(BigDecimal("1234.5")))
        assertEquals("R1 499.00", Formatters.money(BigDecimal("1499.00")))
        assertEquals("R0.00", Formatters.money(BigDecimal.ZERO))
        assertEquals("R999.99", Formatters.money(BigDecimal("999.99")))
    }

    // Verifies large numbers, rounding up to the next thousand, and negative values.
    @Test
    fun money_largeRoundedAndNegative() {
        assertEquals("R1 234 567.89", Formatters.money(BigDecimal("1234567.891")))
        assertEquals("R1 000.00", Formatters.money(BigDecimal("999.999")))
        assertEquals("-R50.00", Formatters.money(BigDecimal("-50")))
    }

    // Verifies API decimal strings parse exactly, and garbage gives null instead of crashing.
    @Test
    fun parseMoney_handlesStringsFromTheApi() {
        assertEquals(BigDecimal("1499.00"), Formatters.parseMoney("1499.00"))
        assertEquals(BigDecimal("10.5"), Formatters.parseMoney(" 10.5 "))
        assertNull(Formatters.parseMoney("abc"))
        assertNull(Formatters.parseMoney(null))
    }

    // Verifies energy values use one decimal and ignore the phone's locale (no decimal commas).
    @Test
    fun kwh_formatsWithOneDecimal() {
        assertEquals("12.5 kWh", Formatters.kwh(12.5))
        assertEquals("12.0 kWh", Formatters.kwh(12.0))
        assertEquals("1234.6 kWh", Formatters.kwh(1234.56))
    }

    // Verifies UTC timestamps are shown in the device time zone.
    @Test
    fun dateTime_convertsFromUtcToLocalZone() {
        assertEquals("30 Sep 2026, 12:00", Formatters.dateTime("2026-09-30T10:00:00Z", johannesburg))
    }

    // Verifies the conversion can move the date across midnight.
    @Test
    fun dateTime_crossesMidnight() {
        assertEquals("1 Oct 2026, 01:30", Formatters.dateTime("2026-09-30T23:30:00Z", johannesburg))
    }

    // Verifies the formats the backend may send (fractional seconds, +00:00) both parse.
    @Test
    fun dateTime_acceptsBackendVariants() {
        assertEquals("30 Sep 2026, 12:00", Formatters.dateTime("2026-09-30T10:00:00.123456Z", johannesburg))
        assertEquals("30 Sep 2026, 12:00", Formatters.dateTime("2026-09-30T10:00:00+00:00", johannesburg))
    }

    // Verifies an unreadable timestamp is shown as-is rather than crashing a screen.
    @Test
    fun dateTime_invalidInputIsReturnedUnchanged() {
        assertEquals("not a date", Formatters.dateTime("not a date"))
        assertEquals("not a date", Formatters.date("not a date"))
    }

    // Verifies plain dates and months used by the energy screens.
    @Test
    fun energyDatesAndMonths() {
        assertEquals("30 Sep", Formatters.shortDate("2026-09-30"))
        assertEquals("Sep 2026", Formatters.month("2026-09"))
        assertEquals("30 Sep 2026", Formatters.date("2026-09-30T10:00:00Z", johannesburg))
        assertEquals("junk", Formatters.shortDate("junk"))
    }

    // Verifies status and role names become readable labels.
    @Test
    fun labels_areHumanReadable() {
        assertEquals("In progress", Formatters.enumLabel("IN_PROGRESS"))
        assertEquals("Pending", Formatters.enumLabel("PENDING"))
        assertEquals("Over limit", Formatters.enumLabel("OVER_LIMIT"))
        assertEquals("Service Provider", Formatters.roleLabel("PROVIDER"))
        assertEquals("Customer", Formatters.roleLabel("CUSTOMER"))
        assertEquals("Business", Formatters.roleLabel("BUSINESS"))
    }

    // Verifies avatar initials for normal, single-word, long and empty names.
    @Test
    fun initials_rules() {
        assertEquals("CC", Formatters.initials("Cathy Customer"))
        assertEquals("M", Formatters.initials("madonna"))
        assertEquals("JM", Formatters.initials("John Michael Doe"))
        assertEquals("AB", Formatters.initials("  ann   bell "))
        assertEquals("?", Formatters.initials("   "))
    }

    // Verifies every sector has a registry entry (new sectors must not be forgotten).
    @Test
    fun sectorRegistry_coversEverySector() {
        assertEquals(Sector.entries.toSet(), SectorRegistry.all.map { it.sector }.toSet())
        assertEquals("Energy Solutions", SectorRegistry.info(Sector.ENERGY).label)
    }
}