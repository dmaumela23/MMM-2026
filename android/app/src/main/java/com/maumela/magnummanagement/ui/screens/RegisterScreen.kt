package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.ui.components.BrandHeader
import com.maumela.magnummanagement.ui.components.MessageCard
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.viewmodel.AuthViewModel

private fun roleDescription(role: UserRole): String = when (role) {
    UserRole.CUSTOMER -> "Browse and order products and services."
    UserRole.PROVIDER -> "Offer services and manage the requests you receive."
    UserRole.BUSINESS -> "Sell products and services and manage incoming orders."
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var roleName by rememberSaveable { mutableStateOf(UserRole.CUSTOMER.name) }
    val role = UserRole.valueOf(roleName)

    LaunchedEffect(state.authenticatedUser) {
        if (state.authenticatedUser != null) {
            viewModel.onAuthenticationHandled()
            onAuthenticated()
        }
    }

    val serverError = state.errorMessage
    val errors = state.registerErrors
    fun submit() = viewModel.register(fullName, email, phone, password, confirm, role)

    MmmScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandHeader()
            Spacer(Modifier.height(8.dp))
            Text("Create your account", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Start))

            if (serverError != null) MessageCard(serverError, isError = true, modifier = Modifier.fillMaxWidth())

            MmmTextField(
                value = fullName, onValueChange = { fullName = it; viewModel.clearErrors() }, label = "Full name",
                error = errors.fullName, capitalization = KeyboardCapitalization.Words, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = email, onValueChange = { email = it; viewModel.clearErrors() }, label = "Email",
                error = errors.email, keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = phone, onValueChange = { phone = it; viewModel.clearErrors() }, label = "Phone number (optional)",
                error = errors.phone, keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = password, onValueChange = { password = it; viewModel.clearErrors() }, label = "Password",
                error = errors.password, helper = "At least 8 characters with a letter and a digit",
                isPassword = true, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = confirm, onValueChange = { confirm = it; viewModel.clearErrors() }, label = "Confirm password",
                error = errors.confirmPassword, isPassword = true, imeAction = ImeAction.Done,
                onImeAction = { submit() }, modifier = Modifier.fillMaxWidth(),
            )

            Text("Account type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.Start)) {
                UserRole.entries.forEach { option ->
                    FilterChip(
                        selected = role == option,
                        onClick = { roleName = option.name },
                        label = { Text(Formatters.roleLabel(option.name)) },
                    )
                }
            }
            Text(
                roleDescription(role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start),
            )

            MmmButton(
                text = "Register",
                onClick = { submit() },
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                Text("Already have an account? Log in")
            }
        }
    }
}
