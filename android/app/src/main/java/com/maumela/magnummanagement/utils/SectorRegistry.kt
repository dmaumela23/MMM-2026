package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.Sector

data class SectorInfo(val sector: Sector, val label: String, val tagline: String)

/**
 * The one place that describes MMM's business sectors. Adding a sector later means adding it to
 * the backend enum and to this list; no screen needs rewriting.
 */
object SectorRegistry {
    val all: List<SectorInfo> = listOf(
        SectorInfo(Sector.DIGITAL, "Digital Products", "Software, licences and digital assets"),
        SectorInfo(Sector.SERVICE, "Service Delivery", "Professional services from trusted providers"),
        SectorInfo(Sector.ENERGY, "Energy Solutions", "Solar, storage and efficiency (usage data is simulated)"),
    )

    fun info(sector: Sector): SectorInfo = all.first { it.sector == sector }
}