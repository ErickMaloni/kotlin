// PERCORSO: ui/receptionist/ReceptionistHomeViewModel.kt
package com.example.esame.ui.receptionist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.BookingStatus
import com.example.esame.data.repository.AuthRepository
import com.example.esame.data.repository.BeachRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // RISOLVE L'ERRORE 'await'
import java.util.*
import javax.inject.Inject

/**
 * Rappresenta lo stato immutabile dell'interfaccia utente per la Dashboard del Receptionist.
 *
 * @property isLoading Indica se i dati della dashboard sono in fase di caricamento.
 * @property userSurname Cognome dell'utente loggato per il messaggio di benvenuto.
 * @property totalUmbrellas Numero totale di ombrelloni censiti.
 * @property freeUmbrellas Numero di ombrelloni disponibili per la data odierna.
 * @property arrivals Conteggio totale degli arrivi previsti oggi (Spiaggia + Bungalow).
 * @property departures Conteggio totale delle partenze previste oggi (Spiaggia + Bungalow).
 * @property arrivalsList Lista descrittiva degli arrivi per il BottomSheet.
 * @property freeBungalows Numero di bungalow disponibili oggi.
 * @property totalBungalows Numero totale di bungalow censiti.
 * @property departuresList Lista descrittiva delle partenze per il BottomSheet.
 */
data class ReceptionistHomeUiState(
    val isLoading: Boolean = true,
    val userSurname: String = "",
    val totalUmbrellas: Int = 0,
    val freeUmbrellas: Int = 0,
    val arrivals: Int = 0,
    val departures: Int = 0,
    val arrivalsList: List<String> = emptyList(),
    val freeBungalows: Int = 0,
    val totalBungalows: Int = 0,
    val departuresList: List<String> = emptyList()
)

/**
 * ViewModel responsabile della logica di business della Dashboard principale.
 *
 * SCOPO:
 * Raccogliere e aggregare dati da diverse fonti (AuthRepository, BeachRepository e Firestore diretto)
 * per fornire una panoramica in tempo reale della situazione della struttura.
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow (UDF)**: Utilizza StateFlow per emettere uno stato atomico alla UI.
 * - **Concurrency**: Utilizza il viewModelScope per eseguire operazioni di rete asincrone in parallelo.
 * - **Date Normalization**: Utilizza una funzione helper per normalizzare le date a mezzanotte UTC,
 *   garantendo confronti precisi tra i timestamp di Firebase e la data odierna.
 */
@HiltViewModel
class ReceptionistHomeViewModel @Inject constructor(
    private val beachRepository: BeachRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceptionistHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserData()
        loadDashboardData()
    }

    /**
     * Recupera i dati del profilo dell'utente corrente.
     * Utilizzato per personalizzare l'intestazione della Home.
     */
    private fun loadUserData() {
        viewModelScope.launch {
            val firebaseUser = authRepository.currentUser
            val user = if (firebaseUser != null) {
                authRepository.getUserData(firebaseUser.uid).firstOrNull()
            } else null
            _uiState.update { it.copy(userSurname = user?.surname ?: "Utente") }
        }
    }

    /**
     * Carica e aggrega i dati statistici della struttura per la giornata corrente.
     *
     * LOGICA:
     * 1. Calcola il timestamp di oggi a mezzanotte.
     * 2. Recupera gli ombrelloni e le relative prenotazioni (tramite Repository).
     * 3. Recupera i bungalow e le relative prenotazioni (tramite Firestore diretto).
     * 4. Filtra i dati per determinare disponibilità, arrivi e partenze.
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val db = FirebaseFirestore.getInstance()

                // DATA DI RIFERIMENTO
                val todayStartOfDay = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    time = Date()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // LOGICA SPIAGGIA
                val allBeachBookings = beachRepository.getAllBookings()
                val totalUmbrellas = beachRepository.getSortedUmbrellasOnce().size

                val occupiedUmbrellasToday = allBeachBookings.filter { booking ->
                    val start = booking.startDate?.toDate()?.startOfDay()
                    val end = booking.endDate?.toDate()?.startOfDay()
                    if (start == null || end == null) false
                    else booking.status == BookingStatus.CONFIRMED && todayStartOfDay.time in start.time..end.time
                }

                // LOGICA BUNGALOW
                val bungalowDocs = db.collection("bungalow").get().await()
                val totalBungalows = bungalowDocs.size()

                val bungalowBookingsQuery = db.collection("bookingsBungalow").get().await()
                val allBungalowBookings = bungalowBookingsQuery.documents // Recuperiamo tutti per filtri successivi

                val occupiedBungalowsToday = allBungalowBookings.count { doc ->
                    val start = doc.getTimestamp("startDate")?.toDate()?.startOfDay()
                    val end = doc.getTimestamp("endDate")?.toDate()?.startOfDay()
                    if (start == null || end == null) false
                    else todayStartOfDay.time in start.time..end.time
                }

                // UNIFICAZIONE ARRIVI E PARTENZE (Spiaggia + Bungalow)

                // Filtro Arrivi
                val beachArrivals = allBeachBookings.filter { it.startDate?.toDate()?.startOfDay() == todayStartOfDay }
                    .map { "Ombrellone ${it.umbrellaId ?: "?"}: ${it.clientName ?: ""} ${it.clientSurname ?: ""}".trim() }

                val bungalowArrivals = allBungalowBookings.filter { it.getTimestamp("startDate")?.toDate()?.startOfDay() == todayStartOfDay }
                    .map { "Bungalow ${it.getString("bungalowId") ?: "?"}: ${it.getString("clientName") ?: ""} ${it.getString("clientSurname") ?: ""}".trim() }

                // Filtro Partenze
                val beachDepartures = allBeachBookings.filter { it.endDate?.toDate()?.startOfDay() == todayStartOfDay }
                    .map { "Ombrellone ${it.umbrellaId ?: "?"}: ${it.clientName ?: ""} ${it.clientSurname ?: ""}".trim() }

                val bungalowDepartures = allBungalowBookings.filter { it.getTimestamp("endDate")?.toDate()?.startOfDay() == todayStartOfDay }
                    .map { "Bungalow ${it.getString("bungalowId") ?: "?"}: ${it.getString("clientName") ?: ""} ${it.getString("clientSurname") ?: ""}".trim() }

                // AGGIORNAMENTO STATO FINALE
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        // Statistiche Ombrelloni
                        totalUmbrellas = totalUmbrellas,
                        freeUmbrellas = (totalUmbrellas - occupiedUmbrellasToday.size).coerceAtLeast(0),

                        // Statistiche Bungalow
                        totalBungalows = totalBungalows,
                        freeBungalows = (totalBungalows - occupiedBungalowsToday).coerceAtLeast(0),

                        // Totali Arrivi/Partenze (Somma di entrambi)
                        arrivals = beachArrivals.size + bungalowArrivals.size,
                        departures = beachDepartures.size + bungalowDepartures.size,

                        // Liste per i BottomSheet
                        arrivalsList = beachArrivals + bungalowArrivals,
                        departuresList = beachDepartures + bungalowDepartures
                    )
                }
            } catch (e: Exception) {
                Log.e("ReceptionistVM", "Errore caricamento dashboard", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Funzione di estensione privata per normalizzare un oggetto Date a mezzanotte UTC.
     * Essenziale per confrontare date senza l'influenza dell'orario o del fuso orario locale.
     */
    private fun Date.startOfDay(): Date {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = this@startOfDay
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}