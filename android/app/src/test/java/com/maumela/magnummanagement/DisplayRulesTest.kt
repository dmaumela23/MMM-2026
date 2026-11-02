package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayRulesTest {

    // Verifies every line status has a colour tone, and finished/cancelled are clearly different.
    @Test
    fun itemStatusTones() {
        assertEquals(Tone.WARNING, DisplayRules.tone(ItemStatus.PENDING))
        assertEquals(Tone.INFO, DisplayRules.tone(ItemStatus.ACCEPTED))
        assertEquals(Tone.INFO, DisplayRules.tone(ItemStatus.IN_PROGRESS))
        assertEquals(Tone.POSITIVE, DisplayRules.tone(ItemStatus.COMPLETED))
        assertEquals(Tone.NEGATIVE, DisplayRules.tone(ItemStatus.CANCELLED))
    }

    // Verifies order and energy statuses map to their tones.
    @Test
    fun orderAndEnergyTones() {
        assertEquals(Tone.WARNING, DisplayRules.tone(OrderStatus.PENDING))
        assertEquals(Tone.INFO, DisplayRules.tone(OrderStatus.PROCESSING))
        assertEquals(Tone.POSITIVE, DisplayRules.tone(OrderStatus.COMPLETED))
        assertEquals(Tone.NEGATIVE, DisplayRules.tone(OrderStatus.CANCELLED))
        assertEquals(Tone.POSITIVE, DisplayRules.tone(EnergyStatus.NORMAL))
        assertEquals(Tone.WARNING, DisplayRules.tone(EnergyStatus.HIGH))
        assertEquals(Tone.NEGATIVE, DisplayRules.tone(EnergyStatus.OVER_LIMIT))
    }

    // Verifies product availability: flag off or no stock blocks ordering, low stock warns.
    @Test
    fun productAvailability_rules() {
        val unavailable = DisplayRules.productAvailability(availability = false, stock = 50)
        assertEquals("Unavailable", unavailable.label)
        assertFalse(unavailable.canOrder)

        val soldOut = DisplayRules.productAvailability(availability = true, stock = 0)
        assertEquals("Out of stock", soldOut.label)
        assertFalse(soldOut.canOrder)

        val low = DisplayRules.productAvailability(availability = true, stock = 3)
        assertEquals("Only 3 left", low.label)
        assertEquals(Tone.WARNING, low.tone)
        assertTrue(low.canOrder)

        val plenty = DisplayRules.productAvailability(availability = true, stock = 6)
        assertEquals("In stock", plenty.label)
        assertEquals(Tone.POSITIVE, plenty.tone)
    }

    // Verifies the low-stock boundary: 5 is "low", 6 is "in stock".
    @Test
    fun productAvailability_lowStockBoundary() {
        assertEquals("Only 5 left", DisplayRules.productAvailability(true, 5).label)
        assertEquals("In stock", DisplayRules.productAvailability(true, 6).label)
    }

    // Verifies services depend only on their availability flag.
    @Test
    fun serviceAvailability_rules() {
        assertTrue(DisplayRules.serviceAvailability(true).canOrder)
        assertFalse(DisplayRules.serviceAvailability(false).canOrder)
        assertEquals("Unavailable", DisplayRules.serviceAvailability(false).label)
    }

    // Verifies star counts round to the nearest whole star and stay within 0 to 5.
    @Test
    fun starsFilled_roundsAndClamps() {
        assertEquals(0, DisplayRules.starsFilled(0.0))
        assertEquals(4, DisplayRules.starsFilled(4.4))
        assertEquals(5, DisplayRules.starsFilled(4.5))
        assertEquals(5, DisplayRules.starsFilled(5.0))
        assertEquals(5, DisplayRules.starsFilled(7.0))
        assertEquals(0, DisplayRules.starsFilled(-2.0))
    }

    // Verifies the delivery text uses the singular for one day.
    @Test
    fun deliveryLabel_pluralisation() {
        assertEquals("Estimated delivery: 1 day", DisplayRules.deliveryLabel(1))
        assertEquals("Estimated delivery: 5 days", DisplayRules.deliveryLabel(5))
    }

    // Verifies the screen-reader summary for charts, including empty and single-reading cases.
    @Test
    fun kwhSummary_forScreenReaders() {
        assertEquals("No usage data", DisplayRules.kwhSummary(emptyList()))
        assertEquals(
            "3 readings: lowest 10.0 kWh, highest 30.0 kWh, average 20.0 kWh",
            DisplayRules.kwhSummary(listOf(10.0, 20.0, 30.0)),
        )
        assertEquals(
            "1 reading: lowest 12.5 kWh, highest 12.5 kWh, average 12.5 kWh",
            DisplayRules.kwhSummary(listOf(12.5)),
        )
    }
}