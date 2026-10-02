package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.ui.theme.ElectricBlue
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.ui.theme.Grey100
import com.maumela.magnummanagement.ui.theme.Navy600
import com.maumela.magnummanagement.ui.theme.Navy700
import com.maumela.magnummanagement.ui.theme.Navy900
import com.maumela.magnummanagement.utils.SectorInfo

private val EnergyAccent = Color(0xFF4FD1A5)

/** A business-sector card (Digital Products, Service Delivery, Energy Solutions) for the Home dashboard. */
@Composable
fun SectorTile(info: SectorInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = when (info.sector) {
        Sector.DIGITAL -> ElectricBlue
        Sector.SERVICE -> Gold
        Sector.ENERGY -> EnergyAccent
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy700),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Navy600, Navy900))),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, accent),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            info.label.first().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = accent,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(info.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(info.tagline, style = MaterialTheme.typography.bodySmall, color = Grey100)
            }
        }
    }
}