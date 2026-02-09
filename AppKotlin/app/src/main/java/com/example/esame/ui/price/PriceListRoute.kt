package com.example.esame.ui.price

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * PriceListRoute funge da "Orchestratore" o "Regista" per la sezione dei listini prezzi.
 *
 * SCOPO:
 * Questa funzione ha il compito di separare la logica di navigazione e la gestione delle
 * dipendenze (Hilt) dalla rappresentazione visiva (PriceListScreen).
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: Estrae lo stato dal ViewModel e lo passa alla Screen sottostante.
 *   Questo rende la `PriceListScreen` una funzione "pura" (stateless), facilitando i test
 *   e la manutenzione.
 * - **Lifecycle Awareness**: Utilizza [collectAsStateWithLifecycle] invece del classico
 *   `collectAsState`. Questa è una best practice di Android che interrompe la raccolta dei
 *   dati quando l'app è in background, ottimizzando il consumo di batteria e risorse.
 * - **Method References (::)**: Utilizza i riferimenti ai metodi del ViewModel per passare
 *   le callback. Questa scelta rende il codice estremamente compatto e leggibile.
 *
 * @param viewModel Istanza del ViewModel fornita automaticamente da Dagger Hilt.
 * @param onBackClick Funzione lambda per gestire la navigazione all'indietro.
 */
@Composable
fun PriceListRoute(
    viewModel: PriceListViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    // Raccoglie lo stato dal ViewModel in modo sicuro rispetto al ciclo di vita.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Passa lo stato e tutte le callback alla PriceListScreen.
    PriceListScreen(
        uiState = uiState,

        // Callback Spiaggia
        // Collega le variazioni dei prezzi settimanali e degli abbonamenti
        onWeeklyPriceChange = viewModel::onWeeklyPriceChange,
        onSubscriptionPriceChange = viewModel::onSubscriptionPriceChange,
        // Gestisce il filtraggio degli elementi visualizzati per la spiaggia
        onBeachFilterSelected = viewModel::onBeachFilterSelected,

        // Callback Bungalow
        // Collega la modifica dei prezzi bungalow (standard/stagionale)
        onBungalowPriceChange = viewModel::onBungalowPriceChange,
        onBungalowSeasonalPriceChange = viewModel::onBungalowSeasonalPriceChange,
        // Gestisce il filtraggio temporale per i listini bungalow
        onBungalowMonthSelected = viewModel::onBungalowMonthSelected,

        // Gestione Tab e Azioni Globali
        // Gestisce lo switch tra i tab Spiaggia e Bungalow
        onTabSelected = viewModel::onTabSelected,
        // Innesca la persistenza dei dati modificati sul database
        onSaveClick = viewModel::onSaveClick,
        // Azione di chiusura della schermata
        onBackClick = onBackClick
    )
}