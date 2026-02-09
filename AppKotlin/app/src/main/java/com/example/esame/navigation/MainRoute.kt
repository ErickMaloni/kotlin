package com.example.esame.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.esame.ui.receptionist.ReceptionistHomeRoute

/**
 * MainRoute funge da orchestratore principale dell'interfaccia utente dopo l'autenticazione.
 *
 * SCOPO:
 * Questa funzione agisce come un router basato sui ruoli. Monitora lo stato dell'utente
 * e determina quale schermata mostrare in base ai permessi salvati nel database.
 *
 * SCELTE ARCHITETTURALI:
 * - **State Hoisting**: Lo stato viene elevato al ViewModel ([MainViewModel]) e osservato qui.
 * - **Separazione dei Ruoli**: Utilizza una logica condizionale per smistare gli utenti verso
 *   le rispettive dashboard (Receptionist o Staff), garantendo sicurezza e pulizia visiva.
 * - **Pattern Route**: Funge da "ponte" tra il sistema di navigazione e le schermate effettive.
 *
 * @param viewModel Il ViewModel globale che gestisce la sessione utente.
 * @param onMappaSpiaggiaClick Callback per la navigazione alla mappa spiaggia.
 * @param onMappaBungalowClick Callback per la navigazione alla mappa bungalow.
 * @param onListinoPrezziClick Callback per visualizzare il listino prezzi.
 * @param onSearchClick Callback per attivare la funzione di ricerca.
 */
@Composable
fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
    onMappaSpiaggiaClick: () -> Unit,
    onMappaBungalowClick: () -> Unit,
    onListinoPrezziClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    // Osservazione dello stato UI in modo reattivo
    val uiState by viewModel.uiState.collectAsState()

    // Logica di rendering condizionale basata sullo stato del caricamento e dell'utente
    when {
        // 1. Stato di caricamento: Mostra un indicatore di progresso durante il recupero dei dati utente
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // 2. Utente autenticato: Smistamento in base al ruolo ricevuto da Firestore
        uiState.user != null -> {
            val user = uiState.user!!

            when {
                // Caso Receptionist: Accesso alla dashboard completa
                user.role.equals("Receptionist", ignoreCase = true) -> {
                    ReceptionistHomeRoute(
                        onMappaSpiaggiaClick = onMappaSpiaggiaClick,
                        onMappaBungalowClick = onMappaBungalowClick,
                        onLogout = { viewModel.onLogoutClick() },
                        onListinoPrezziClick = onListinoPrezziClick,
                        onSearchClick = onSearchClick
                    )
                }

                // Caso Staff (Spiaggia o Bungalow): Accesso a interfacce limitate
                user.role.equals("Staff Spiaggia", ignoreCase = true) -> {
                    StaffScreen(
                        roleName = "Staff Spiaggia",
                        onLogoutClick = { viewModel.onLogoutClick() }
                    )
                }

                user.role.equals("Staff Bungalow", ignoreCase = true) -> {
                    StaffScreen(
                        roleName = "Staff Bungalow",
                        onLogoutClick = { viewModel.onLogoutClick() }
                    )
                }

                // Caso Fallback: Ruolo esistente nel DB ma non ancora implementato nell'App
                else -> {
                    ErrorScreen(
                        message = "Il ruolo '${user.role}' non è stato configurato nel sistema.",
                        onLogoutClick = { viewModel.onLogoutClick() }
                    )
                }
            }
        }
    }
}

/**
 * Schermata di cortesia per i membri dello staff.
 *
 * Utilizzata come segnaposto (placeholder) per i ruoli che non hanno ancora
 * una dashboard dedicata, garantendo che l'utente non rimanga bloccato.
 */
@Composable
private fun StaffScreen(
    roleName: String,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Benvenuto, $roleName!",
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogoutClick) {
            Text("Esegui Logout")
        }
    }
}

/**
 * Schermata di gestione degli errori per l'identità utente.
 *
 * Viene mostrata se i dati nel profilo utente (es. ruolo mancante o errato)
 * impediscono la corretta navigazione nelle aree riservate.
 */
@Composable
private fun ErrorScreen(
    message: String,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️ Attenzione",
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogoutClick) {
            Text("Esegui Logout")
        }
    }
}