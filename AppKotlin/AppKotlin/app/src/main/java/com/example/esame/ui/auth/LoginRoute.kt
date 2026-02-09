package com.example.esame.ui.auth

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Questa è la "Route" per la schermata di login.
 * Fa da ponte tra la logica di navigazione e la UI,
 * gestendo gli "effetti collaterali" come la navigazione e i Toast.
 */
@Composable
fun LoginRoute(
    loginViewModel: LoginViewModel = hiltViewModel(),
    // Callback per notificare al NavHost di navigare DOPO il successo
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Questo blocco reagisce al cambio di stato 'isAuthenticated'
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess() // Esegue la navigazione definita in AppNavigation
        }
    }

    // Chiama la Composable che si occupa solo di disegnare la UI
    LoginScreen(
        uiState = uiState,
        onEmailChange = loginViewModel::onEmailChange,
        onPasswordChange = loginViewModel::onPasswordChange,
        onSignInClick = loginViewModel::signIn,
        onNavigateToRegister = onNavigateToRegister,
        onForgotPasswordClick = loginViewModel::onForgotPasswordClick,
        onErrorMessageShown = loginViewModel::onErrorMessageShown,
        onPasswordResetEmailSentDismissed = loginViewModel::onPasswordResetEmailSentDismissed
    )
}
