package com.maumela.magnummanagement.ui.screens

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.ui.components.BrandHeader
import com.maumela.magnummanagement.ui.components.MessageCard
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.authenticatedUser) {
        if (state.authenticatedUser != null) {
            viewModel.onAuthenticationHandled()
            onAuthenticated()
        }
    }

    val serverError = state.errorMessage
    val errors = state.loginErrors
    fun launchGoogleSignIn() {
        val rawNonce = UUID.randomUUID().toString()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("123833307045-rtgeot50v3iud32hk3pga1ulnkoj3q5o.apps.googleusercontent.com")
            .setNonce(rawNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Call the ViewModel method that talks to your real backend endpoint
                    viewModel.googleSignIn(idToken)
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "Google Sign-in failed", e)
            }
        }
    }

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

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val webClientId = "123833307045-rtgeot50v3iud32hk3pga1ulnkoj3q5o.apps.googleusercontent.com"

        LaunchedEffect(state.authenticatedUser) {
            if (state.authenticatedUser != null) {
                viewModel.onAuthenticationHandled()
                onAuthenticated()
            }
        }

        val serverError = state.errorMessage
        val errors = state.loginErrors

        fun launchGoogleSignIn() {
            val rawNonce = UUID.randomUUID().toString()
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(rawNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                    val credential = result.credential
                    if (credential is androidx.credentials.CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        viewModel.googleSignIn(googleIdTokenCredential.idToken)
                    }
                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignIn", "Google Sign-in failed", e)
                }
            }
        }

        MmmScaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                BrandHeader(subtitle = "Products · Services · Energy")
                Spacer(Modifier.height(16.dp))
                Text(
                    "Log in",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (notice != null) MessageCard(
                    notice,
                    isError = false,
                    modifier = Modifier.fillMaxWidth()
                )
                if (serverError != null) MessageCard(
                    serverError,
                    isError = true,
                    modifier = Modifier.fillMaxWidth()
                )

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

                // Google SSO Button
                MmmButton(
                    text = "Sign in with Google",
                    onClick = { launchGoogleSignIn() },
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextButton(onClick = onRegister, enabled = !state.isLoading) {
                    Text("New to MMM? Create an account")
                }
            }
        }
    }
}