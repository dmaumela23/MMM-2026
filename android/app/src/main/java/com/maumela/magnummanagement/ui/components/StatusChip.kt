package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.ui.theme.StatusAmber
import com.maumela.magnummanagement.ui.theme.StatusBlue
import com.maumela.magnummanagement.ui.theme.StatusGreen
import com.maumela.magnummanagement.ui.theme.StatusGrey
import com.maumela.magnummanagement.ui.theme.StatusRed
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.Tone

private fun Tone.color(): Color = when (this) {
    Tone.POSITIVE -> StatusGreen
    Tone.WARNING -> StatusAmber
    Tone.NEGATIVE -> StatusRed
    Tone.INFO -> StatusBlue
    Tone.NEUTRAL -> StatusGrey
}

/** A coloured dot + text label. The label carries the meaning, so colour is never the only signal. */
@Composable
fun StatusChip(label: String, tone: Tone, modifier: Modifier = Modifier) {
    val color = tone.color()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ItemStatusChip(status: ItemStatus, modifier: Modifier = Modifier) =
    StatusChip(Formatters.enumLabel(status.name), DisplayRules.tone(status), modifier)

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) =
    StatusChip(Formatters.enumLabel(status.name), DisplayRules.tone(status), modifier)

@Composable
fun EnergyStatusChip(status: EnergyStatus, modifier: Modifier = Modifier) =
    StatusChip(Formatters.enumLabel(status.name), DisplayRules.tone(status), modifier)