package com.maumela.magnummanagement

import com.maumela.magnummanagement.navigation.Routes
import com.maumela.magnummanagement.viewmodel.MarketplaceTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    // Verifies detail screens build their address from an id.
    @Test
    fun detailRoutes_includeTheId() {
        assertEquals("product/7", Routes.product(7))
        assertEquals("service/12", Routes.service(12))
        assertEquals("order/3", Routes.order(3))
    }

    // Verifies the marketplace address is plain when no tab or search is given.
    @Test
    fun marketplace_withoutArguments_isPlain() {
        assertEquals("marketplace", Routes.marketplace())
        assertEquals("marketplace", Routes.marketplace(query = "   "))
    }

    // Verifies a tab and a search text are carried as arguments.
    @Test
    fun marketplace_carriesTabAndQuery() {
        assertEquals("marketplace?tab=SERVICES", Routes.marketplace(tab = MarketplaceTab.SERVICES))
        assertEquals("marketplace?q=solar", Routes.marketplace(query = " solar "))
        assertEquals("marketplace?tab=PRODUCTS&q=solar", Routes.marketplace(MarketplaceTab.PRODUCTS, "solar"))
    }

    // Verifies spaces become %20 (Navigation does not decode "+") and symbols are escaped.
    @Test
    fun marketplace_encodesTheQuerySafely() {
        assertEquals("marketplace?q=solar%20panel", Routes.marketplace(query = "solar panel"))
        assertEquals("marketplace?q=100%25%20%26%20more", Routes.marketplace(query = "100% & more"))
    }

    // Verifies tab arguments are read case-insensitively, defaulting to Products.
    @Test
    fun tabFromArgument() {
        assertEquals(MarketplaceTab.SERVICES, MarketplaceTab.fromArg("SERVICES"))
        assertEquals(MarketplaceTab.SERVICES, MarketplaceTab.fromArg("services"))
        assertEquals(MarketplaceTab.PRODUCTS, MarketplaceTab.fromArg(null))
        assertEquals(MarketplaceTab.PRODUCTS, MarketplaceTab.fromArg("nonsense"))
    }

    // Verifies the bottom bar shows on the five top-level destinations only.
    @Test
    fun bottomBar_showsOnTopLevelScreens() {
        for (route in listOf(Routes.HOME, Routes.MARKETPLACE, Routes.ENERGY, Routes.ORDERS, Routes.PROFILE)) {
            assertTrue(route, Routes.showsBottomBar(route))
        }
        assertTrue(Routes.showsBottomBar("marketplace?tab=SERVICES"))
    }

    // Verifies it is hidden on auth screens, detail screens and sub-pages.
    @Test
    fun bottomBar_hiddenElsewhere() {
        for (route in listOf(
            Routes.SPLASH, Routes.LOGIN, Routes.REGISTER, Routes.PRODUCT, Routes.SERVICE,
            Routes.ORDER, Routes.ENERGY_USAGE, Routes.ENERGY_REPORTS, Routes.SETTINGS,
        )) {
            assertFalse(route, Routes.showsBottomBar(route))
        }
        assertFalse(Routes.showsBottomBar(null))
    }
}