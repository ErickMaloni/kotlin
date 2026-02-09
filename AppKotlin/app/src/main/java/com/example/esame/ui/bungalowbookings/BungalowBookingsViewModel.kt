package com.example.esame.ui.bungalowbookings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.Bungalow
import com.example.esame.data.model.BungalowBooking
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Stato della UI
/**
 * Rappresenta lo stato immutabile dell'interfaccia utente per la gestione dei Bungalow.
 *
 * DESIGN CHOICE: L'utilizzo di una singola data class per lo stato (UDF - Unidirectional Data Flow)
 * garantisce che la UI sia una funzione pura dello stato, facilitando il debug e la consistenza dei dati.
 */
data class BungalowBookingsUiState(
    val bungalows: List<Bungalow> = emptyList(),
    val bookings: List<BungalowBooking> = emptyList(),
    val isLoading: Boolean = true,
    val toastMessage: String? = null,
    val showBookingDialog: Boolean = false,
    val selectedCellData: Pair<String, Int>? = null,
    val previewPrice: Double = 0.0,
    val showBeachPriorityDialog: Boolean = false,
    val lastSavedBooking: BungalowBooking? = null,
    val selectedBooking: BungalowBooking? = null,
    val showInfoSheet: Boolean = false,
    val showDetailsSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val bookingsBeach: List<com.example.esame.data.model.Booking> = emptyList(),
    val umbrellasBeach: List<com.example.esame.data.model.Umbrella> = emptyList()

)


/**
 * ViewModel responsabile della logica di business per le prenotazioni dei Bungalow.
 *
 * SCOPO:
 * Gestire il caricamento dei dati da Firestore, il calcolo della disponibilità,
 * la gestione dei dialoghi UI e le operazioni CRUD (Create, Read, Delete, Update).
 *
 * SCELTE ARCHITETTURALI:
 * - **Hilt**: Utilizzato per la Dependency Injection dell'istanza Firestore.
 * - **StateFlow**: Utilizzato per emettere lo stato in modo reattivo alla UI.
 * - **Coroutines**: Tutte le chiamate di rete sono asincrone per non bloccare il Main Thread.
 */
@HiltViewModel
class BungalowBookingsViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BungalowBookingsUiState())
    val uiState = _uiState.asStateFlow()

    // Data di riferimento per la visualizzazione del tabellone (Stato di Compose)
    private val _currentDate = mutableStateOf(Calendar.getInstance().apply {
        set(2026, Calendar.MAY, 15)
    })

    init {
        loadData()
    }

    /**
     * Esegue il caricamento iniziale dei bungalow e imposta un ascoltatore in tempo reale
     * per le prenotazioni tramite [addSnapshotListener].
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bungalowsSnapshot = db.collection("bungalow").get().await()
                val bungalows = bungalowsSnapshot.mapNotNull { doc ->
                    doc.toObject(Bungalow::class.java).copy(id = doc.id)
                }

                db.collection("bookingsBungalow").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _uiState.update { it.copy(toastMessage = "Errore snapshot: ${error.message}") }
                        return@addSnapshotListener
                    }

                    val bookingsList = snapshot?.mapNotNull { doc ->
                        doc.toObject(BungalowBooking::class.java).copy(bookingId = doc.id)
                    } ?: emptyList()

                    _uiState.update {
                        it.copy(bungalows = bungalows, bookings = bookingsList, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, toastMessage = "Errore caricamento: ${e.message}") }
            }
        }
    }
}