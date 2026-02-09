package com.example.esame.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esame.ui.theme.EsameTheme

/**
 * Colori definiti localmente
*/
private val PrimaryColor = Color(0xFF1976D2)
private val InputBgColor = Color(0xFFF3F4F6)

/**
 * Schermata di Login principale dell'applicazione.
 *
 * SCOPO:
 * Fornire un'interfaccia intuitiva e moderna per l'autenticazione degli utenti.
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow**: La funzione è "stateless", riceve i dati dallo stato [uiState]
 *   e comunica verso l'alto tramite callback.
 * - **Layering Visivo**: Utilizza un Box per sovrapporre uno sfondo decorativo a un pannello
 *   inferiore (Surface) stile "Bottom Sheet" per un look moderno.
 * - **Feedback Utente**: Gestisce dialoghi di errore e conferma reset password direttamente dalla UI.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onErrorMessageShown: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onPasswordResetEmailSentDismissed: () -> Unit
) {
    // Stato locale per la visibilità della password (UI-only state)
    var isPasswordVisible by remember { mutableStateOf(false) }

    // GESTIONE DIALOGHI

    // Dialogo per conferma invio email di reset
    if (uiState.passwordResetEmailSent) {
        AlertDialog(
            onDismissRequest = onPasswordResetEmailSentDismissed,
            title = { Text("Email Inviata") },
            text = { Text("Controlla la tua casella di posta per resettare la password.") },
            confirmButton = {
                Button(onClick = onPasswordResetEmailSentDismissed) { Text("OK") }
            }
        )
    }

    // Dialogo per la visualizzazione di errori bloccanti
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onErrorMessageShown,
            title = { Text("Errore") },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                Button(onClick = onErrorMessageShown) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1976D2), Color(0x855B91E8))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = (-80).dp)
                    .size(250.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = 50.dp)
                    .size(150.dp)
                    .background(Color(0xFF70A5E8).copy(alpha = 0.2f), CircleShape)
            )
        }

        // PANNELLO DEI CONTENUTI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Intestazione
                    Text(
                        text = "Bentornato",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Accedi per gestire la struttura",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 8.dp, bottom = 32.dp)
                    )

                    // Campo Email
                    ModernTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        icon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Password con toggle visibilità
                    ModernTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onVisibilityChange = { isPasswordVisible = !isPasswordVisible }
                    )

                    // Link Password Dimenticata
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = onForgotPasswordClick) {
                            Text(
                                text = "Password dimenticata?",
                                color = PrimaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Azione di Accesso
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Accedi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Navigazione verso la registrazione
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Non hai un account? ", color = Color.Gray)
                        Text(
                            text = "Registrati",
                            color = PrimaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente personalizzato per i campi di testo dell'app.
 *
 * SCELTA TECNICA:
 * Centralizzare lo stile dei TextField permette di modificare il design
 * dell'intera interfaccia di input da un unico punto, garantendo coerenza
 * tra Email e Password.
 *
 * @param isPassword Se true, applica trasformazioni visuali e tastiera specifica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityChange: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryColor
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onVisibilityChange) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryColor,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = InputBgColor,
            unfocusedContainerColor = InputBgColor,
            disabledContainerColor = InputBgColor
        ),
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    EsameTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSignInClick = {},
            onNavigateToRegister = {},
            onErrorMessageShown = {},
            onForgotPasswordClick = {},
            onPasswordResetEmailSentDismissed = {}
        )
    }
}