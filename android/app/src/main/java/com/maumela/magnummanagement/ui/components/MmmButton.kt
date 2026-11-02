package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.ui.theme.Navy900

enum class MmmButtonStyle { Primary, Secondary, Danger }

/**
 * The app's one button. Primary = gold call to action, Secondary = outlined, Danger = destructive.
 * While [loading] it shows a spinner and ignores taps, so a request cannot be sent twice.
 */
@Composable
fun MmmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MmmButtonStyle = MmmButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonModifier = modifier.heightIn(min = 48.dp) // accessible touch target
    val canClick = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }

    when (style) {
        MmmButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = canClick,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Navy900,
                disabledContainerColor = Gold.copy(alpha = 0.4f),
                disabledContentColor = Navy900.copy(alpha = 0.6f),
            ),
            content = content,
        )
        MmmButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = canClick,
            shape = shape,
            content = content,
        )
        MmmButtonStyle.Danger -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = canClick,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            content = content,
        )
    }
}