package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/** A product image loaded from its URL (Coil). Shows a placeholder while loading and if it fails. */
@Composable
fun ListingImage(url: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        ImagePlaceholder(modifier)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null, // decorative: the card's text already names the product
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { ImagePlaceholder(Modifier.fillMaxSize(), showSpinner = true) },
            error = { ImagePlaceholder(Modifier.fillMaxSize()) },
        )
    }
}

@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, showSpinner: Boolean = false) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showSpinner) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}