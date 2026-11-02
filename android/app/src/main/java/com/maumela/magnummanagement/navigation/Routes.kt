package com.maumela.magnummanagement.navigation

import com.maumela.magnummanagement.viewmodel.MarketplaceTab
import java.net.URLEncoder

/** Every screen address in one place. Patterns contain {arguments}; helper functions fill them in. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    const val ARG_ID = "id"
    const val ARG_TAB = "tab"
    const val ARG_QUERY = "q"

    // Marketplace holds the Products and Services tabs. Both arguments are optional.
    const val MARKETPLACE_BASE = "marketplace"
    const val MARKETPLACE = "marketplace?tab={tab}&q={q}"

    const val PRODUCT = "product/{id}"
    const val SERVICE = "service/{id}"
    const val ENERGY = "energy"
    const val ENERGY_USAGE = "energy/usage"
    const val ENERGY_REPORTS = "energy/reports"
    const val ORDERS = "orders"
    const val ORDER = "order/{id}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    /** The five destinations that show the bottom bar. */
    val bottomBarRoutes: Set<String> = setOf(HOME, MARKETPLACE_BASE, ENERGY, ORDERS, PROFILE)

    fun product(id: Int) = "product/$id"
    fun service(id: Int) = "service/$id"
    fun order(id: Int) = "order/$id"

    /**
     * Address of the marketplace, optionally pre-selecting a tab and a search text.
     * With no arguments it is plain "marketplace", which keeps the user's current search.
     */
    fun marketplace(tab: MarketplaceTab? = null, query: String? = null): String {
        val parts = buildList {
            if (tab != null) add("$ARG_TAB=${tab.name}")
            if (!query.isNullOrBlank()) add("$ARG_QUERY=${encode(query.trim())}")
        }
        return if (parts.isEmpty()) MARKETPLACE_BASE else "$MARKETPLACE_BASE?${parts.joinToString("&")}"
    }

    /** "marketplace?tab={tab}&q={q}" -> "marketplace" */
    fun baseRoute(route: String?): String? = route?.substringBefore('?')

    fun showsBottomBar(route: String?): Boolean = baseRoute(route) in bottomBarRoutes

    // Spaces must be %20 (not "+"), because Navigation does not decode "+".
    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}