package com.example.esame.ui.search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.type
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.esame.ui.search.SearchViewModel

/**
 * SearchRoute funge da "Orchestratore" (o Regista) per la funzionalità di ricerca globale.
 *
 * SCOPO:
 * Questa funzione agisce come un ponte (Bridge) tra il sistema di navigazione,
 * la logica di business gestita dal [SearchViewModel] e la rappresentazione visiva [SearchScreen].
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: Estrae lo stato dal ViewModel e lo passa verso il basso (downward).
 *   Questo permette alla [SearchScreen] di essere una funzione "pura" (stateless),
 *   facilitando i test e la manutenzione.
 * - **Dependency Injection (Hilt)**: L'istanza del ViewModel viene iniettata automaticamente
 *   tramite [hiltViewModel], garantendo che il ciclo di vita sia legato correttamente alla navigazione.
 * - **Reactive UI**: Utilizza [collectAsState] per trasformare i flussi di dati (Flow) del ViewModel
 *   in stati di Compose, garantendo che la UI si aggiorni automaticamente ad ogni digitazione dell'utente.
 *
 * @param viewModel Istanza del ViewModel fornita da Hilt.
 * @param onBackClick Callback per gestire la navigazione all'indietro.
 */
@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    // Raccoglie i risultati della ricerca
    val searchResults by viewModel.searchResults.collectAsState()

    // Raccoglie il testo di ricerca attuale
    val searchText by viewModel.searchText.collectAsState()

    // RENDERING DELLA UI
    // Chiama la Composable della UI aggiornata
    SearchScreen(
        searchText = searchText,
        onSearchTextChanged = viewModel::onSearchTextChanged,
        searchResults = searchResults, // Passa la lista unificata (Spiaggia + Bungalow)
        onBackClick = onBackClick,
        onResultClick = { result ->
            // Qui puoi gestire cosa succede quando si clicca su un risultato
            // Esempio: navigare al dettaglio della prenotazione
            println("Cliccato su: ${result.name} - Tipo: ${result.type}")
        }
    )
}