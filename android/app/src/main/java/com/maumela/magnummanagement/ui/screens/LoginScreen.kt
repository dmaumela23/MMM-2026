package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.ui.components.BrandHeader
import com.maumela.magnummanagement.ui.components.MessageCard
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.viewmodel.AuthViewModel

/** [notice] is a non-error message such as "Your session expired. Please log in again." */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    notice: String?,
    onAuthenticated: () -> Unit,
    onRegister: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.authenticatedUser) {
        if (state.authenticatedUser != null) {
            viewModel.onAuthenticationHandled()
            onAuthenticated()
        }
    }

    val serverError = state.errorMessage
    val errors = state.loginErrors

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
            Spacer(Modifier.height(24.dp))
            BrandHeader(subtitle = "Products · Services · Energy")
            Spacer(Modifier.height(16.dp))
            Text("Log in", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Start))

            if (notice != null) MessageCard(notice, isError = false, modifier = Modifier.fillMaxWidth())
            if (serverError != null) MessageCard(serverError, isError = true, modifier = Modifier.fillMaxWidth())

            MmmTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearErrors() },
                label = "Email",
                error = errors.email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearErrors() },
                label = "Password",
                error = errors.password,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
            )
            MmmButton(
                text = "Log in",
                onClick = { viewModel.login(email, password) },
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onRegister, enabled = !state.isLoading) {
                Text("New to MMM? Create an account")
            }
        }
    }
}
