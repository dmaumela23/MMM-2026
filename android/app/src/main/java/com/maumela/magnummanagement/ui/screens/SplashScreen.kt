package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.ui.theme.Grey200Dark
import com.maumela.magnummanagement.ui.theme.Navy700
import com.maumela.magnummanagement.ui.theme.Navy800
import com.maumela.magnummanagement.ui.theme.Navy900
import com.maumela.magnummanagement.viewmodel.SplashState
import com.maumela.magnummanagement.viewmodel.SplashViewModel

/** Brand intro. Meanwhile the saved login (if any) is checked with the server. */
@Composable
fun SplashScreen(viewModel: SplashViewModel, onGoHome: () -> Unit, onGoLogin: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        when (state) {
            SplashState.GoToHome -> onGoHome()
            SplashState.GoToLogin -> onGoLogin()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy800, Navy700))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("MMM", style = MaterialTheme.typography.displayMedium, color = Gold)
            Text("MAUMELA MAGNUM MANAGEMENT", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Products · Services · Energy", style = MaterialTheme.typography.bodyMedium, color = Grey200Dark)
            Spacer(Modifier.height(32.dp))

            val current = state
            when (current) {
                SplashState.Checking -> CircularProgressIndicator(color = Gold)
                is SplashState.Error -> {
                    Text(current.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    MmmButton("Try again", onClick = viewModel::check)
                }
                else -> Unit
            }
        }
    }
}
