package com.example.esame.ui.receptionist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * ReceptionistHomeRoute funge da "Orchestratore" per la Home Page del Receptionist.
 *
 * SCOPO:
 * Questa funzione agisce come un ponte (Bridge) tra il sistema di navigazione
 * e la logica di business gestita dal [ReceptionistHomeViewModel].
 * Separa la gestione delle dipendenze (Hilt) dalla rappresentazione visiva (Screen).
 *
 * DESIGN CHOICES:
 * - **Pattern Route/Screen**: Segue la best practice di Jetpack Compose che prevede
 *   una "Route" per la gestione dello stato e uno "Screen" per la UI pura (stateless).
 * - **State Hoisting**: Estrae lo stato dal ViewModel e lo passa verso il basso (downward),
 *   permettendo alla [ReceptionistHomeScreen] di essere facilmente testabile e riutilizzabile.
 * - **Dependency Injection (Hilt)**: L'istanza del ViewModel viene iniettata automaticamente
 *   tramite [hiltViewModel], garantendo il corretto ciclo di vita legato alla navigazione.
 *
 * @param onMappaSpiaggiaClick Callback per navigare alla gestione spiaggia.
 * @param onMappaBungalowClick Callback per navigare alla gestione bungalow.
 * @param onListinoPrezziClick Callback per navigare alla gestione listini.
 * @param onLogout Callback per gestire l'uscita dell'utente.
 * @param onSearchClick Callback per avviare la funzionalità di ricerca globale.
 * @param viewModel Istanza del ViewModel fornita da Hilt.
 */
@Composable
fun ReceptionistHomeRoute(
    // Parametri per la navigazione
    onMappaSpiaggiaClick: () -> Unit,
    onMappaBungalowClick: () -> Unit,
    onListinoPrezziClick: () -> Unit,
    onLogout: () -> Unit,
    onSearchClick: () -> Unit,
    // ViewModel per la logica
    viewModel: ReceptionistHomeViewModel = hiltViewModel()
) {
    // Raccoglie lo stato dal ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Passa lo stato e le funzioni allo Screen che si occupa della UI
    ReceptionistHomeScreen(
        uiState = uiState,
        onMappaSpiaggiaClick = onMappaSpiaggiaClick,
        onMappaBungalowClick = onMappaBungalowClick,
        onListinoPrezziClick = onListinoPrezziClick,
        onLogout = onLogout,
        onSearchClick = onSearchClick
    )
}
