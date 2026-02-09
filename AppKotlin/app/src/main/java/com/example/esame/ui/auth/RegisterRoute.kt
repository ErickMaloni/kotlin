package com.example.esame.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * RegisterRoute funge da orchestratore tra la logica di navigazione e la logica di business
 * per la creazione di un nuovo account.
 *
 * SCOPO:
 * 1. Collegare il [RegisterViewModel] alla relativa interfaccia grafica [RegisterScreen].
 * 2. Gestire gli "Side Effects" (effetti collaterali) come la navigazione automatica
 *    e la visualizzazione di dialoghi informativi.
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: Lo stato della UI viene sollevato nel ViewModel e osservato qui come
 *   uno stato reattivo. Questo separa la gestione dei dati dalla loro rappresentazione.
 * - **LaunchedEffect**: Utilizzato per reagire ai cambiamenti di stato critici (es. registrazione riuscita).
 *   È la scelta migliore per eseguire azioni di navigazione in modo sicuro rispetto al ciclo di vita di Compose.
 * - **Separazione delle responsabilità**: La Route gestisce "cosa succede" (navigazione, dialoghi),
 *   mentre la Screen gestisce "come appare" l'interfaccia.
 */
@Composable
fun RegisterRoute(
    viewModel: RegisterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    // Osservazione dello stato UI globale del modulo di registrazione
    val uiState by viewModel.uiState.collectAsState()

    // Stato locale per gestire la visibilità del dialogo di successo
    var showSuccessDialog by remember { mutableStateOf(false) }

    /**
     * GESTIONE DEL SUCCESSO DELLA REGISTRAZIONE
     * Quando il flag [registrationSuccess] diventa true nel ViewModel,
     * attiviamo la visualizzazione del dialogo di conferma.
     */
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            showSuccessDialog = true
        }
    }

    /**
     * DIALOGO DI CONFERMA
     * Fornisce un feedback importante all'utente: informa che l'account è stato creato
     * ma richiede la verifica dell'email prima di poter procedere.
     *
     * SCELTA TECNICA: Utilizziamo un AlertDialog per garantire che l'utente legga
     * le istruzioni di verifica prima di essere reindirizzato.
     */
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onRegistrationSuccess() // Navigazione verso il login post-chiusura
            },
            title = {
                Text(text = "Registrazione Completata!")
            },
            text = {
                Text(text = "Ti abbiamo inviato una mail di conferma. Per favore, verifica il tuo indirizzo email prima di accedere.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onRegistrationSuccess() // Navigazione verso il login post-click
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    /**
     * Componente UI puro (Screen).
     * Riceve lo stato e le callback per gestire gli input dell'utente.
     * Usiamo i riferimenti a funzione (::) per mantenere il codice pulito e leggibile.
     */
    RegisterScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onSurnameChange = viewModel::onSurnameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRoleChange = viewModel::onRoleChange,
        onRegisterClick = viewModel::registerUser,
        onNavigateBack = onNavigateBack,
        onErrorMessageShown = viewModel::onErrorMessageShown
    )
}