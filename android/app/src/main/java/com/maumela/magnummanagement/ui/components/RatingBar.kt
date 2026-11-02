package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.utils.DisplayRules
import java.util.Locale

/** Read-only star rating (ratings are stored values in the prototype; there are no reviews yet). */
@Composable
fun RatingBar(rating: Double, modifier: Modifier = Modifier, starSize: Dp = 16.dp) {
    val filled = DisplayRules.starsFilled(rating)
    val text = if (rating > 0) String.format(Locale.US, "%.1f", rating) else "No rating"
    val description = if (rating > 0) "Rated $text out of 5" else "Not rated yet"

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < filled) Gold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(starSize),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}