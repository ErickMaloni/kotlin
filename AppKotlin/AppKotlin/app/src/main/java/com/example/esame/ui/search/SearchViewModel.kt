package com.example.esame.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.Booking
import com.example.esame.data.model.BungalowBooking
import com.example.esame.data.model.Umbrella
import com.example.esame.data.repository.BeachRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// MODELLI DATI
/**
 * Rappresenta un singolo risultato di ricerca unificato.
 *
 * SCOPO: Permette alla UI di visualizzare in un'unica lista sia prenotazioni
 * della spiaggia che dei bungalow, astraendo le differenze dei modelli originali.
 */
data class SearchResult(
    val id: String,
    val name: String,
    val surname: String,
    val location: String,
    val dateRange: String, // Es: "Ombrellone 15" o "Bungalow B3"
    val type: SearchType
)

/**
 * Identifica la categoria del risultato per permettere alla UI di mostrare
 * icone o colori differenti.
 */
enum class SearchType { BEACH, BUNGALOW }

/**
 * SearchViewModel gestisce la logica di business per la ricerca globale.
 *
 * SCELTE ARCHITETTURALI:
 * - **Debounce**: Utilizzato per evitare ricerche superflue ad ogni singolo tasto premuto,
 *   migliorando le performance.
 * - **StateFlow**: Garantisce che la UI sia sempre sincronizzata con l'ultimo stato dei dati.
 * - **Caching Locale**: I dati vengono caricati una volta all'avvio in [_allResultsRaw]
 *   per rendere il filtraggio istantaneo durante la digitazione.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val beachRepository: BeachRepository,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _allResultsRaw = MutableStateFlow<List<SearchResult>>(emptyList())

    /**
     * Risultati filtrati esposti alla UI.
     *
     * LOGICA:
     * - [debounce]: Attende 300ms di inattività prima di procedere.
     * - [combine]: Unisce il testo di ricerca con la lista completa dei risultati.
     * - [stateIn]: Converte il Flow in uno Stato caldo per la UI, mantenendolo in memoria
     *   per 5 secondi se l'app va in background.
     */
    val searchResults: StateFlow<List<SearchResult>> = _searchText
        .debounce(300L)
        .combine(_allResultsRaw) { text, allResults ->
            if (text.isBlank()) {
                emptyList()
            } else {
                allResults.filter {
                    it.name.contains(text, ignoreCase = true) ||
                            it.surname.contains(text, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadInitialData()
    }

    /**
     * Carica tutte le prenotazioni esistenti (Spiaggia e Bungalow) da Firebase.
     *
     * PROCESSO:
     * 1. Recupera anagrafica ombrelloni per ottenere i numeri reali.
     * 2. Recupera anagrafica bungalow per mappare gli ID con i nomi/numeri.
     * 3. Trasforma le prenotazioni Spiaggia nel modello [SearchResult].
     * 4. Trasforma le prenotazioni Bungalow nel modello [SearchResult].
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                //  Recupera Ombrelloni
                val allUmbrellas = beachRepository.getSortedUmbrellasOnce()

                // Recupera Bungalow e mappa ID -> Number
                val bungalowSnapshot = db.collection("bungalow").get().await()
                val bungalowMap = bungalowSnapshot.documents.associate { doc ->
                    val displayValue = doc.get("number")?.toString() ?: doc.id
                    doc.id to displayValue
                }

                // Mappa prenotazioni SPIAGGIA
                val beachBookingsMapped = beachRepository.getAllBookings().map { b ->
                    val umbrellaNumber = allUmbrellas.find { it.id == b.umbrellaId }?.number?.toString() ?: "?"
                    SearchResult(
                        id = b.id ?: "",
                        name = b.clientName ?: "",
                        surname = b.clientSurname ?: "",
                        location = "Ombrellone: $umbrellaNumber",
                        dateRange = "${formatDate(b.startDate)} - ${formatDate(b.endDate)}", // <--- DATA AGGIUNTA
                        type = SearchType.BEACH
                    )
                }

                // Mappa prenotazioni BUNGALOW
                val bungalowBookingsSnapshot = db.collection("bookingsBungalow").get().await()
                val bungalowBookingsMapped = bungalowBookingsSnapshot.documents.mapNotNull { doc ->
                    val bId = doc.getString("bungalowId")
                    val displayId = bungalowMap[bId] ?: bId ?: "?"

                    SearchResult(
                        id = doc.id,
                        name = doc.getString("clientName") ?: "",
                        surname = doc.getString("clientSurname") ?: "",
                        location = "Bungalow: $displayId",
                        dateRange = "${formatDate(doc.getTimestamp("startDate"))} - ${formatDate(doc.getTimestamp("endDate"))}", // <--- DATA AGGIUNTA
                        type = SearchType.BUNGALOW
                    )
                }

                _allResultsRaw.value = beachBookingsMapped + bungalowBookingsMapped

            } catch (e: Exception) {
                Log.e("SearchVM", "Errore nel caricamento dati: ${e.message}")
            }
        }
    }

    // FUNZIONE HELPER PER FORMATTARE LE DATE
    private fun formatDate(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return "??"
        val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.ITALIAN)
        return sdf.format(timestamp.toDate())
    }

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }
}