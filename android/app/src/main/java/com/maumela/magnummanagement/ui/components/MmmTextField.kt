package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * The app's one text input. [error] (a message from Validators) turns the field red and
 * is shown underneath. Password fields get a Show/Hide button.
 */
@Composable
fun MmmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    enabled: Boolean = true,
    isPassword: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val supporting = error ?: helper

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        enabled = enabled,
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        supportingText = supporting?.let {
            {
                Text(
                    it,
                    color = if (error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
        else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = {
                focusManager.clearFocus()
                onImeAction?.invoke()
            },
            onGo = { onImeAction?.invoke() },
            onSearch = { onImeAction?.invoke() },
        ),
    )
}