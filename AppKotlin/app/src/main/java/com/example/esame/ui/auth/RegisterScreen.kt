package com.example.esame.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Schermata di registrazione dell'utente.
 *
 * SCOPO:
 * Fornire un'interfaccia per la creazione di un nuovo account.
 *
 * DESIGN CHOICES:
 * - **Sfondo Dinamico**: Utilizza un gradiente verticale e cerchi decorativi trasparenti
 *   per garantire coerenza visiva con la schermata di Login.
 * - **Stateless UI**: Riceve lo stato e notifica gli eventi tramite callback.
 * - **Accessibilità**: Il contenuto è avvolto in un [verticalScroll] per gestire tastiere aperte o schermi piccoli.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onErrorMessageShown: () -> Unit
) {
    // GESTIONE DIALOGHI DI ERRORE
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onErrorMessageShown,
            title = { Text(text = "Attenzione") },
            text = { Text(text = uiState.errorMessage) },
            confirmButton = {
                TextButton(onClick = onErrorMessageShown) {
                    Text("OK")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. NUOVO SFONDO DECORATIVO (Coerente con Login) ---
        // Gradiente principale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1976D2), Color(0x855B91E8))
                    )
                )
        ) {
            // Cerchio decorativo superiore sinistro
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = (-80).dp)
                    .size(250.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            // Cerchio decorativo superiore destro
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = 50.dp)
                    .size(150.dp)
                    .background(Color(0xFF70A5E8).copy(alpha = 0.2f), CircleShape)
            )
        }

        // --- 2. CONTENUTO DELLA SCHERMATA ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                // verticalScroll permette di scorrere i campi quando la tastiera copre lo schermo
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Spazio iniziale per bilanciare il contenuto rispetto allo sfondo
            Spacer(Modifier.height(60.dp))

            // Intestazione
            Text(
                text = "Crea un Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            // CONFIGURAZIONE STILE CAMPI DI TESTO
            val textFieldColors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White
            )
            val fieldShape = RoundedCornerShape(50)

            // Input: Nome
            TextField(
                value = uiState.name,
                onValueChange = onNameChange,
                placeholder = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Input: Cognome
            TextField(
                value = uiState.surname,
                onValueChange = onSurnameChange,
                placeholder = { Text("Cognome") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Input: Email
            TextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Input: Password
            TextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = textFieldColors,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(16.dp))

            // Input: Conferma Password
            TextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = { Text("Conferma Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = fieldShape,
                colors = textFieldColors,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(16.dp))

            // SELEZIONE RUOLO (Dropdown Menu)
            var expanded by remember { mutableStateOf(false) }
            val roles = listOf("Receptionist")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = uiState.role,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Ruolo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = fieldShape,
                    colors = textFieldColors
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                onRoleChange(role)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // AREA AZIONI
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = fieldShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(
                        text = "REGISTRATI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Link per tornare al Login
            TextButton(onClick = onNavigateBack) {
                Text(
                    text = "Hai già un account? Accedi",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Spazio extra per lo scrolling
            Spacer(Modifier.height(32.dp))
        }
    }
}