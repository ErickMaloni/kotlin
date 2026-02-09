package com.example.esame.ui.bookings

import android.content.Context
import androidx.compose.animation.core.copy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.drawText
import androidx.core.text.color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.Booking
import com.example.esame.data.model.BookingStatus
import com.example.esame.data.model.Umbrella
import com.example.esame.data.model.Bungalow
import com.example.esame.data.model.BungalowBooking
import com.example.esame.data.repository.BeachRepository
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
import java.io.File
import java.io.FileOutputStream
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import android.graphics.pdf.PdfDocument // <--- DEVE ESSERE QUESTO
import android.util.Log

import java.util.*

/**
 * Stato della UI per la gestione delle prenotazioni (Spiaggia e Bungalow).
 *
 * DESIGN CHOICE: L'utilizzo di una singola Data Class per lo stato (UiState) segue il pattern
 * Unidirectional Data Flow (UDF). Questo centralizza la gestione dei dati e rende la UI
 * più prevedibile e facile da testare.
 */
data class BookingsUiState(
    val umbrellas: List<Umbrella> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val pendingBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val toastMessage: String? = null,
    val showBookingDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmationDialog: Boolean = false, // Spiaggia
    val showBookingOptionsDialog: Boolean = false,
    val showPendingBookingsSheet: Boolean = false,
    val selectedCellData: Pair<String, Int>? = null,
    val selectedBookingForOptions: Booking? = null,
    val bookingToRebook: Booking? = null,
    val previewPrice: Double = 0.0,
    val showInfoDialog: Boolean = false,
    val bungalowBookings: List<BungalowBooking> = emptyList(),
    val bungalows: List<Bungalow> = emptyList(),
    val bookingsBeach: List<Booking> = emptyList(),
    val umbrellasBeach: List<Umbrella> = emptyList(),
    val isEditMode: Boolean = false,
    val showDeleteConfirmation: Boolean = false, // Bungalow
    val showDetailsSheet: Boolean = false,
    val showBeachPriorityDialog: Boolean = false,
    val lastSavedBooking: BungalowBooking? = null,
    val selectedBooking: BungalowBooking? = null,
    val showInfoSheet: Boolean = false
)


/**
 * ViewModel responsabile della coordinazione tra i dati del database (Firestore/Repository)
 * e la visualizzazione del tabellone prenotazioni.
 *
 * SCELTE TECNICHE:
 * - @HiltViewModel: Iniezione automatica delle dipendenze per scalabilità e testing.
 * - StateFlow: Gestione reattiva dello stato che garantisce la persistenza dei dati durante la rotazione.
 * - viewModelScope: Assicura che le operazioni asincrone vengano cancellate se la UI viene distrutta.
 */
@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val beachRepository: BeachRepository,
    private val db: FirebaseFirestore
) : ViewModel() {

    // Stato interno mutabile (Privato)
    private val _uiState = MutableStateFlow(BookingsUiState())

    // Stato esposto alla UI (Immutabile)
    val uiState = _uiState.asStateFlow()

    // Variabile per tracciare se stiamo collegando una
    // prenotazione spiaggia a un bungalow
    private var sourceBungalowBookingId: String? = null

    // Stato per la data corrente visualizzata nel tabellone (Default Maggio 2026)
    private val _currentDate = mutableStateOf(
        Calendar.getInstance().apply { set(2026, Calendar.MAY, 15) }
    )
    val currentDate: State<Calendar> = _currentDate

    init {
        loadData()
    }

    // CARICAMENTO DATI
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Recupero Ombrelloni e Bungalow (Anagrafica)
                val umbrellasResult = beachRepository.getSortedUmbrellasOnce()
                val bungalowsSnapshot = db.collection("bungalow").get().await()
                val bungalowsList = bungalowsSnapshot.mapNotNull { it.toObject(Bungalow::class.java).copy(id = it.id) }

                // 2. Listener in tempo reale per le prenotazioni spiaggia
                db.collection("bookings").addSnapshotListener { beachSnapshot, _ ->
                    val beachList = beachSnapshot?.mapNotNull { it.toObject(Booking::class.java).copy(id = it.id) } ?: emptyList()

                    // 3. Listener in tempo reale per le prenotazioni bungalow
                    db.collection("bookingsBungalow").addSnapshotListener { bungalowSnapshot, _ ->
                        val bungalowList = bungalowSnapshot?.mapNotNull { it.toObject(BungalowBooking::class.java).copy(bookingId = it.id) } ?: emptyList()

                        // Aggiorniamo lo stato con tutto sincronizzato
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                umbrellas = umbrellasResult,
                                umbrellasBeach = umbrellasResult,
                                bungalows = bungalowsList,
                                bookingsBeach = beachList, // Tutte le prenotazioni spiaggia
                                bungalowBookings = bungalowList,
                                // Filtri per il tabellone spiaggia
                                bookings = beachList.filter { b -> b.status == BookingStatus.CONFIRMED || b.status == null },
                                pendingBookings = beachList.filter { b -> b.status == BookingStatus.PENDING }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, toastMessage = e.message) }
            }
        }
    }

    // LOGICA SPIAGGIA
    /**
     * Salva una nuova prenotazione spiaggia e genera contestualmente la ricevuta PDF.
     */
    fun onSaveBooking(context: Context, clientName: String, clientSurname: String, endDate: Date, isSeasonal: Boolean) {
        viewModelScope.launch {
            try {
                val state = _uiState.value

                // Identifichiamo l'ID dell'ombrellone
                val umbrellaId = if (state.isEditMode) state.selectedBookingForOptions?.umbrellaId else state.selectedCellData?.first
                if (umbrellaId == null) return@launch

                // 1. Calcolo Data Inizio e Fine Normalizzate
                val calStart = Calendar.getInstance().apply {
                    if (state.isEditMode && state.selectedBookingForOptions != null) {
                        time = state.selectedBookingForOptions.startDate?.toDate() ?: Date()
                    } else {
                        val baseCal = _currentDate.value.clone() as Calendar
                        time = baseCal.time
                        set(Calendar.DAY_OF_MONTH, state.selectedCellData?.second ?: 1)
                    }
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                val calEnd = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }

                val startTs = Timestamp(calStart.time)
                val endTs = Timestamp(calEnd.time)

                // --- 2. CONTROLLO DISPONIBILITÀ (OVERLAP) ---
                // Verifichiamo se ci sono prenotazioni che "sbattono" contro quella nuova
                val existingBookingsSnapshot = db.collection("bookings")
                    .whereEqualTo("umbrellaId", umbrellaId)
                    .get()
                    .await()

                val isOccupied = existingBookingsSnapshot.documents.any { doc ->
                    // Se siamo in modifica, ignoriamo la prenotazione stessa che stiamo spostando
                    if (state.isEditMode && doc.id == state.selectedBookingForOptions?.id) return@any false

                    val bStart = doc.getTimestamp("startDate") ?: return@any false
                    val bEnd = doc.getTimestamp("endDate") ?: return@any false

                    // Logica Overlap: (Inizio_A <= Fine_B) && (Fine_A >= Inizio_B)
                    val overlaps = startTs.seconds <= bEnd.seconds && endTs.seconds >= bStart.seconds
                    overlaps
                }

                if (isOccupied) {
                    _uiState.update { it.copy(toastMessage = "Ombrellone occupato nei giorni seguenti!") }
                    return@launch // Blocca l'esecuzione e non salva
                }
                // --------------------------------------------

                // 3. Preparazione oggetto Booking
                val bookingData = Booking(
                    umbrellaId = umbrellaId,
                    clientName = clientName,
                    clientSurname = clientSurname,
                    startDate = startTs,
                    endDate = endTs,
                    totalPrice = state.previewPrice,
                    seasonal = isSeasonal,
                    status = BookingStatus.CONFIRMED
                )

                val beachBookingId: String
                if (state.isEditMode && state.selectedBookingForOptions?.id != null) {
                    // MODIFICA
                    beachBookingId = state.selectedBookingForOptions.id!!
                    db.collection("bookings").document(beachBookingId).set(bookingData).await()
                } else {
                    // NUOVA PRENOTAZIONE
                    val beachDocRef = db.collection("bookings").add(bookingData).await()
                    beachBookingId = beachDocRef.id

                    sourceBungalowBookingId?.let { bungalowId ->
                        db.collection("bookingsBungalow").document(bungalowId)
                            .update("linkedUmbrellaBookingId", beachBookingId).await()
                        sourceBungalowBookingId = null
                    }
                }

                // 4. GENERAZIONE PDF
                val umbrella = state.umbrellas.find { it.id == umbrellaId }
                val rowIndex = umbrella?.rowIndex?.toString() ?: "N/D"
                val umbrellaNumber = umbrella?.number?.toString() ?: "N/D"
                generateReceiptPdf(context, bookingData.copy(id = beachBookingId), umbrellaNumber, rowIndex)

                // 5. Reset UI
                _uiState.update { it.copy(showBookingDialog = false, isEditMode = false, toastMessage = "Prenotazione salvata!") }
                loadData()

            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Errore: ${e.message}") }
            }
        }
    }
    /**
     * Calcola il preventivo dinamico basato sul listino prezzi (periodo e fila dell'ombrellone).
     */
    /** Calcola il preventivo dinamico basato sul listino prezzi (periodo e fila dell'ombrellone).
    */
    fun calculatePricePreview(endDate: Date, isSeasonal: Boolean) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                // 1. Identificazione Ombrellone
                val umbrellaId = if (state.isEditMode) state.selectedBookingForOptions?.umbrellaId else state.selectedCellData?.first
                if (umbrellaId == null) return@launch

                val umbrella = state.umbrellas.find { it.id == umbrellaId } ?: return@launch
                val umbrellaRow = umbrella.rowIndex.toLong()

                // 2. Determinazione Data Inizio (Normalizzata a mezzanotte)
                val calStart = Calendar.getInstance().apply {
                    if (state.isEditMode && state.selectedBookingForOptions != null) {
                        time = state.selectedBookingForOptions.startDate?.toDate() ?: Date()
                    } else if (state.selectedCellData != null) {
                        val baseCal = _currentDate.value.clone() as Calendar
                        time = baseCal.time
                        set(Calendar.DAY_OF_MONTH, state.selectedCellData.second)
                    }
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                var price = 0.0

                if (isSeasonal) {
                    val seasonalSnapshot = db.collection("price").whereEqualTo("period", "SEASONAL").get().await()
                    val seasonalDoc = seasonalSnapshot.documents.find { doc ->
                        val sRow = doc.getLong("startRow") ?: 0L
                        val eRow = doc.getLong("endRow") ?: 99L
                        umbrellaRow in sRow..eRow
                    }
                    price = seasonalDoc?.getDouble("price") ?: 0.0
                } else {
                    // 3. Mese in Inglese (Deve corrispondere a "JULY" nel DB)
                    val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(calStart.time).uppercase()

                    val priceSnapshot = db.collection("price").whereEqualTo("period", monthName).get().await()
                    val priceDoc = priceSnapshot.documents.find { doc ->
                        val sRow = doc.getLong("startRow") ?: 0L
                        val eRow = doc.getLong("endRow") ?: 99L
                        umbrellaRow in sRow..eRow
                    }

                    if (priceDoc == null) {
                        Log.e("PriceError", "Nessun listino trovato per fila $umbrellaRow nel mese $monthName")
                        _uiState.update { it.copy(previewPrice = 0.0) }
                        return@launch
                    }

                    // 4. Calcolo Giorni
                    val calEnd = Calendar.getInstance().apply {
                        time = endDate
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val diffMillis = calEnd.timeInMillis - calStart.timeInMillis
                    val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1

                    // 5. Ricerca della tariffa giornaliera nella mappa "weeks"
                    val weeksMap = priceDoc.get("weeks") as? Map<String, Any>
                    var foundDailyRate: Double? = null

                    weeksMap?.values?.forEach { weekData ->
                        val week = weekData as? Map<String, Any>
                        val wStart = (week?.get("startDate") as? Timestamp)?.toDate()
                        val wEnd = (week?.get("endDate") as? Timestamp)?.toDate()

                        if (wStart != null && wEnd != null) {
                            // Normalizziamo le date del DB per il confronto
                            val dbStart = Calendar.getInstance().apply { time = wStart; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
                            val dbEnd = Calendar.getInstance().apply { time = wEnd; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.time

                            if (!calStart.time.before(dbStart) && !calStart.time.after(dbEnd)) {
                                val dayOfWeek = calStart.get(Calendar.DAY_OF_WEEK)
                                val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

                                foundDailyRate = if (isWeekend) {
                                    (week["holidayPrice"] as? Number)?.toDouble()
                                } else {
                                    (week["weekdayPrice"] as? Number)?.toDouble()
                                }
                            }
                        }
                    }

                    price = days * (foundDailyRate ?: 0.0)
                    if (foundDailyRate == null) Log.e("PriceError", "Data ${calStart.time} non coperta da nessuna week nel listino")
                }

                _uiState.update { it.copy(previewPrice = price) }

            } catch (e: Exception) {
                Log.e("PriceError", "Errore: ${e.message}", e)
                _uiState.update { it.copy(previewPrice = 0.0) }
            }
        }
    }

    fun calculateBungalowPricePreview(
        start: Date,
        bungalowId: String,
        numPeople: Int,
        hasPets: Boolean,
        isSeasonal: Boolean,
        endDate: Date
    ) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val bungalow = state.bungalows.find { it.id == bungalowId } ?: return@launch

                // GESTIONE STAGIONALE (Prezzo Fisso)
                // --- GESTIONE STAGIONALE (Prezzo Fisso con Animali) ---
                if (isSeasonal) {
                    val priceSnapshot = db.collection("priceBungalow")
                        .whereEqualTo("period", "SEASONAL")
                        .get()
                        .await()

                    val priceDoc = priceSnapshot.documents.firstOrNull()

                    if (priceDoc != null && priceDoc.exists()) {
                        val priceMap = priceDoc.get("price") as? Map<String, Any>

                        // 1. Impostiamo la chiave fissa su "standard_2" come richiesto
                        val priceKey = "standard_2"

                        val baseSeasonalPrice = (priceMap?.get(priceKey) as? Number)?.toDouble() ?: 0.0

                        // 2. Aggiungiamo il supplemento animali (+20€) se lo switch è attivo
                        val petsFee = if (hasPets) 20.0 else 0.0

                        val finalSeasonalPrice = baseSeasonalPrice + petsFee

                        _uiState.update { it.copy(previewPrice = finalSeasonalPrice) }
                    } else {
                        _uiState.update { it.copy(previewPrice = 0.0, toastMessage = "Listino Stagionale non trovato") }
                    }
                    return@launch
                }

                // GESTIONE SETTIMANALE DINAMICA (Somma dei periodi)
                var totalBasePrice = 0.0

                // Calcoliamo quanti giorni totali dura il soggiorno
                val diffMillis = endDate.time - start.time
                val totalDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1

                // Calcoliamo quante settimane (o frazioni) ci sono
                val numberOfWeeks = Math.ceil(totalDays / 7.0).toInt()

                // Ciclo per ogni settimana della prenotazione
                for (i in 0 until numberOfWeeks) {
                    // Data di riferimento per questa specifica settimana
                    val currentWeekDate = Calendar.getInstance().apply {
                        time = start
                        add(Calendar.DAY_OF_YEAR, i * 7)
                    }.time

                    val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(currentWeekDate).uppercase()

                    // Cerchiamo il listino del mese in cui cade QUESTA settimana
                    val priceDoc = db.collection("priceBungalow")
                        .whereEqualTo("period", monthName)
                        .get().await().documents.firstOrNull()

                    val weeks = priceDoc?.get("weeks") as? Map<String, Map<String, Any>>
                    var weekPriceFound = 0.0

                    weeks?.values?.forEach { weekData ->
                        val wStart = (weekData["startDate"] as? Timestamp)?.toDate()
                        val wEnd = (weekData["endDate"] as? Timestamp)?.toDate()

                        if (wStart != null && wEnd != null && !currentWeekDate.before(wStart) && !currentWeekDate.after(wEnd)) {
                            val pMap = weekData["price"] as? Map<String, Any>
                            val key = when {
                                bungalow.type == "DELUXE" -> "deluxe"
                                numPeople == 2 -> "standard_2"
                                numPeople == 3 -> "standard_3"
                                numPeople == 4 -> "family_4"
                                numPeople >= 5 -> "family_5"
                                else -> "standard_2"
                            }
                            weekPriceFound = (pMap?.get(key) as? Number)?.toDouble() ?: 0.0
                        }
                    }
                    totalBasePrice += weekPriceFound
                }

                val cleaningFee = 15.0
                val petsFee = if (hasPets && !isSeasonal) 20.0 else 0.0
                val finalTotal = totalBasePrice + cleaningFee + petsFee

                _uiState.update { it.copy(previewPrice = finalTotal) }

            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Errore calcolo: ${e.message}") }
            }
        }
    }


    /**
     * Salva o modifica una prenotazione Bungalow, normalizzando gli orari di Check-in (12:00) e Check-out (10:00).
     */
    fun onSaveBungalowBooking(
        name: String,
        surname: String,
        endDate: Date,
        numPeople: Int,
        hasPets: Boolean,
        isSeasonal: Boolean
    ) {
        viewModelScope.launch {try {
            val state = _uiState.value
            val bId = if (state.isEditMode) state.selectedBooking?.bungalowId else state.selectedCellData?.first ?: return@launch

            // NORMALIZZAZIONE DELLE DATE
            val finalStartDate: Timestamp
            val finalEndDate: Timestamp

            if (isSeasonal) {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val startCal = Calendar.getInstance().apply { set(year, Calendar.MAY, 15, 12, 0, 0) }
                val endCal = Calendar.getInstance().apply { set(year, Calendar.SEPTEMBER, 15, 23, 59, 59) }
                finalStartDate = Timestamp(startCal.time)
                finalEndDate = Timestamp(endCal.time)
            } else {
                // Normalizzazione INIZIO (Check-in ore 12:00)
                val calStart = (if (state.isEditMode) state.selectedBooking?.startDate?.toDate() else {
                    _uiState.value.selectedCellData?.let { (_, day) ->
                        (_currentDate.value.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.time
                    }
                }) ?: Date()

                val normalizedStartCal = Calendar.getInstance().apply {
                    time = calStart
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                finalStartDate = Timestamp(normalizedStartCal.time)

                // Normalizzazione FINE (Check-out ore 10:00)
                // Questo permette il cambio ospite lo stesso giorno (uno esce alle 10, l'altro entra alle 12)
                val normalizedEndCal = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, 10)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                finalEndDate = Timestamp(normalizedEndCal.time)
            }

            // CONTROLLO DISPONIBILITÀ
            // Verifica che non ci siano sovrapposizioni (escludendo se stessi in modalità edit)
            val isAvailable = isBungalowAvailable(
                bungalowId = bId!!,
                start = finalStartDate,
                end = finalEndDate,
                ignoreBookingId = if (state.isEditMode) state.selectedBooking?.bookingId else null
            )

            if (!isAvailable) {
                _uiState.update { it.copy(toastMessage = "ATTENZIONE: Bungalow già occupato in queste date!") }
                return@launch
            }

            // CREAZIONE OGGETTO E SALVATAGGIO
            val bookingData = BungalowBooking(
                bookingId = if (state.isEditMode) state.selectedBooking?.bookingId else null,
                bungalowId = bId,
                clientName = name,
                clientSurname = surname,
                startDate = finalStartDate,
                endDate = finalEndDate,
                numPeople = numPeople,
                hasPets = if (isSeasonal) false else hasPets,
                isSeasonal = isSeasonal,
                totalPrice = state.previewPrice, // Prezzo dinamico calcolato (es. 350+350+400)
                status = "Occupato"
            )

            if (state.isEditMode) {
                db.collection("bookingsBungalow")
                    .document(bookingData.bookingId!!)
                    .set(bookingData)
                    .await()
            } else {
                val ref = db.collection("bookingsBungalow").add(bookingData).await()
                _uiState.update {
                    it.copy(
                        lastSavedBooking = bookingData.copy(bookingId = ref.id),
                        showBeachPriorityDialog = true
                    )
                }
            }

            _uiState.update { it.copy(showBookingDialog = false, toastMessage = "Prenotazione salvata con successo!") }
            loadData() // Ricarica la griglia per vedere il cambiamento

        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Errore salvataggio: ${e.message}") }
        }
        }
    }

    // Funzione di supporto per la disponibilità (se non già presente nel file)
    private fun isBungalowAvailable(bungalowId: String, start: Timestamp, end: Timestamp, ignoreBookingId: String?): Boolean {
        val existingBookings = _uiState.value.bungalowBookings.filter {
            it.bungalowId == bungalowId && it.bookingId != ignoreBookingId
        }
        return existingBookings.none { existing ->
            val exStart = existing.startDate?.seconds ?: 0L
            val exEnd = existing.endDate?.seconds ?: 0L
            val newStart = start.seconds
            val newEnd = end.seconds
            // Formula standard di sovrapposizione: (InizioA < FineB) AND (FineA > InizioB)
            newStart < exEnd && newEnd > exStart
        }
    }



    // GESTIONE CELLE & RIALLOCAZIONE
    fun onCellClick(umbrellaId: String, day: Int) {
        val toRebook = _uiState.value.bookingToRebook
        if (toRebook != null) {
            viewModelScope.launch {
                val newStart = (_currentDate.value.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, day); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time
                val duration = toRebook.endDate!!.toDate().time - toRebook.startDate!!.toDate().time
                val newEnd = Date(newStart.time + duration)

                if (checkAvailability(umbrellaId, newStart, newEnd, toRebook.id)) {
                    db.collection("bookings").document(toRebook.id!!)
                        .update("umbrellaId", umbrellaId, "startDate", Timestamp(newStart), "endDate", Timestamp(newEnd), "status", BookingStatus.CONFIRMED).await()
                    _uiState.update { it.copy(bookingToRebook = null, toastMessage = "Riallocato correttamente!") }
                    loadData()
                } else {
                    _uiState.update { it.copy(toastMessage = "ATTENZIONE: Sovrapposizione!") }
                }
            }
        } else {
            _uiState.update { it.copy(selectedCellData = Pair(umbrellaId, day), showBookingDialog = true, previewPrice = 0.0) }
        }
    }

    private fun checkAvailability(umbrellaId: String, start: Date, end: Date, ignoreId: String?): Boolean {
        val umbrellaBookings = _uiState.value.bookingsBeach.filter { it.umbrellaId == umbrellaId && it.id != ignoreId && it.status != BookingStatus.PENDING }
        return umbrellaBookings.none { existing ->
            val exStart = existing.startDate?.toDate() ?: return@none false
            val exEnd = existing.endDate?.toDate() ?: return@none false
            start.before(exEnd) && end.after(exStart)
        }
    }

    // METODI UI UTILITY
    fun onBookingOptionsClick(booking: Booking) { _uiState.update { it.copy(selectedBookingForOptions = booking, showBookingOptionsDialog = true) } }
    fun onBookingOptionsClick(booking: BungalowBooking) { _uiState.update { it.copy(selectedBooking = booking, showInfoSheet = true) } }

    fun onInfoClick() { _uiState.update { it.copy(showBookingOptionsDialog = false, showInfoDialog = true) } }
    fun onDeleteClick() {
        val bungalowBooking = _uiState.value.selectedBooking
        val beachBooking = _uiState.value.selectedBookingForOptions

        if (bungalowBooking != null) {
            // Se è un bungalow, apriamo il dialog di conferma bungalow
            _uiState.update { it.copy(
                showDeleteConfirmation = true,
                showInfoSheet = false
            ) }
        } else if (beachBooking != null) {
            // Se è una spiaggia, apriamo il dialog di conferma spiaggia
            _uiState.update { it.copy(
                showDeleteConfirmationDialog = true,
                showBookingOptionsDialog = false
            ) }
        }
    }
    fun onEditClick() { _uiState.update { it.copy(showBookingOptionsDialog = false, showBookingDialog = true, isEditMode = true) } }

    fun deleteBooking() {
        val b = _uiState.value.selectedBookingForOptions ?: return
        viewModelScope.launch {
            try {
                // Cancella dal DB spiaggia (bookings)
                db.collection("bookings").document(b.id!!).delete().await()

                _uiState.update { it.copy(
                    showDeleteConfirmationDialog = false,
                    selectedBookingForOptions = null,
                    toastMessage = "Prenotazione spiaggia eliminata"
                ) }

                // Ricarica la griglia spiaggia
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Errore: ${e.message}") }
            }
        }
    }

    fun onConfirmDelete() {
        val bungalowBooking = _uiState.value.selectedBooking
        val beachBooking = _uiState.value.selectedBookingForOptions

        viewModelScope.launch {try {
            if (bungalowBooking != null) {
                // CANCELLAZIONE BUNGALOW
                db.collection("bookingsBungalow")
                    .document(bungalowBooking.bookingId!!)
                    .delete()
                    .await()

                _uiState.update { it.copy(showDeleteConfirmation = false, selectedBooking = null) }
            } else if (beachBooking != null) {
                // CANCELLAZIONE SPIAGGIA
                db.collection("bookings")
                    .document(beachBooking.id!!)
                    .delete()
                    .await()

                _uiState.update { it.copy(showDeleteConfirmationDialog = false, selectedBookingForOptions = null) }
            }

            loadData()
            _uiState.update { it.copy(toastMessage = "Eliminato con successo") }

        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Errore: ${e.message}") }
        }
        }
    }
    fun placeBookingOnHold() {
        val booking = _uiState.value.selectedBookingForOptions ?: return
        viewModelScope.launch {
            db.collection("bookings").document(booking.id!!).update("status", BookingStatus.PENDING).await()
            _uiState.update { it.copy(showBookingOptionsDialog = false, toastMessage = "In attesa") }
            loadData()
        }
    }


// Genera Ricevuta PDF
fun generateReceiptPdf(context: Context, booking: Booking, umbrellaNumber: String, row: String) {
    // Usiamo la classe nativa per evitare conflitti con androidx.pdf
    val pdfDocument = android.graphics.pdf.PdfDocument()

    // Formato A4
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)

    val canvas: android.graphics.Canvas = page.canvas
    val paint = android.graphics.Paint()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    // INTESTAZIONE
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 22f
    paint.color = android.graphics.Color.BLACK
    canvas.drawText("STABILIMENTO BALNEARE", 50f, 80f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.textSize = 12f
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("RICEVUTA PRENOTAZIONE", 380f, 80f, paint)

    paint.color = android.graphics.Color.BLACK
    paint.strokeWidth = 2f
    canvas.drawLine(50f, 95f, 545f, 95f, paint)

    // DETTAGLI CLIENTE
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 14f
    canvas.drawText("DETTAGLI CLIENTE", 50f, 140f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.textSize = 13f
    canvas.drawText("Nome: ${booking.clientName}", 50f, 165f, paint)
    canvas.drawText("Cognome: ${booking.clientSurname}", 50f, 185f, paint)

    // DETTAGLI POSTAZIONE
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("DETTAGLI POSTAZIONE", 50f, 230f, paint)

    // Rettangolo box postazione (Grigio chiaro)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.LTGRAY
    paint.strokeWidth = 1f
    canvas.drawRoundRect(50f, 245f, 350f, 340f, 10f, 10f, paint)

    // Testo nel box
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    canvas.drawText("Ombrellone Numero: $umbrellaNumber", 65f, 275f, paint)
    canvas.drawText("Fila: $row", 65f, 295f, paint)

    val dateStart = booking.startDate?.toDate() ?: Date()
    val dateEnd = booking.endDate?.toDate() ?: Date()
    canvas.drawText("Periodo: ${sdf.format(dateStart)} - ${sdf.format(dateEnd)}", 65f, 315f, paint)

    // TOTALE
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 20f
    val totalText = "TOTALE PAGATO: Euro ${String.format("%.2f", booking.totalPrice)}"
    canvas.drawText(totalText, 250f, 400f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    paint.textSize = 11f
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Grazie per averci scelto! Ci vediamo in spiaggia.", 160f, 800f, paint)

    pdfDocument.finishPage(page)

    // SALVATAGGIO FILE
    val fileName = "Ricevuta_${booking.clientSurname}_${System.currentTimeMillis()}.pdf"
    // FIX: Usa android.os.Environment invece di com.google.ai...
    val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), fileName)

    try {
        pdfDocument.writeTo(FileOutputStream(file))

        // CODICE PER APRIRE IL PDF AUTOMATICAMENTE
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        // ----------------------------------------------------

        _uiState.update { it.copy(toastMessage = "Ricevuta generata!") }
    } catch (e: Exception) {
        _uiState.update { it.copy(toastMessage = "Errore creazione PDF: ${e.message}") }
    } finally {
        pdfDocument.close()
    }

}

    /**
     * Passa dalla visualizzazione riassuntiva (InfoSheet) ai dettagli completi del bungalow.
     * Scelta: Si chiude il foglio informativo prima di aprire i dettagli per evitare sovrapposizioni visive.
     */
    fun onShowDetailsClick() { _uiState.update { it.copy(showInfoSheet = false, showDetailsSheet = true) } }

    /**
     * Attiva la modalità di modifica per una prenotazione esistente.
     * Scelta: [isEditMode] a true permette alla UI di NewBookingDialog di pre-caricare i dati esistenti.
     */
    fun onEditBookingClick() { _uiState.update { it.copy(showInfoSheet = false, showBookingDialog = true, isEditMode = true) } }

    //  FUNZIONI DI CHIUSURA
    // Queste funzioni garantiscono la pulizia dello stato quando l'utente chiude un componente UI
    fun onDismissDialog() { _uiState.update { it.copy(showBookingDialog = false) } }
    fun onDismissBookingOptionsDialog() { _uiState.update { it.copy(showBookingOptionsDialog = false, showInfoSheet = false) } }
    fun onDismissInfoDialog() { _uiState.update { it.copy(showInfoDialog = false, showDetailsSheet = false) } }
    fun onDismissDeleteDialog() { _uiState.update { it.copy(showDeleteConfirmationDialog = false, showDeleteConfirmation = false) } }
    fun onDismissDetailsSheet() { _uiState.update { it.copy(showDetailsSheet = false) } }
    fun onDismissBeachPriority() { _uiState.update { it.copy(showBeachPriorityDialog = false) } }

    /**
     * Resetta il messaggio Toast dopo la visualizzazione.
     * Scelta: Fondamentale per evitare che lo stesso messaggio ricompaia in caso di ricomposizione della UI.
     */
    fun onToastMessageShown() { _uiState.update { it.copy(toastMessage = null) } }

    // GESTIONE LISTA D'ATTESA E RIALLOCAZIONE
    fun onShowPendingBookingsClick() { _uiState.update { it.copy(showPendingBookingsSheet = true) } }
    fun onDismissPendingBookingsSheet() { _uiState.update { it.copy(showPendingBookingsSheet = false) } }


    /**
     * Inizia la procedura di spostamento (rebooking) di una prenotazione dalla lista d'attesa.
     * Scelta: Salva la prenotazione in [bookingToRebook] per permettere all'utente di selezionare una nuova cella sulla griglia.
     */
    fun startRebookingProcess(booking: Booking) { _uiState.update { it.copy(bookingToRebook = booking, showPendingBookingsSheet = false) } }

    /**
     * Annulla l'operazione di spostamento in corso, riportando la UI allo stato normale.
     */
    fun cancelRebooking() { _uiState.update { it.copy(bookingToRebook = null) } }


    /**
     * Collega logicamente una prenotazione spiaggia a una bungalow esistente tramite ID.
     */
    fun setSourceBungalow(id: String?) { this.sourceBungalowBookingId = id }

    /**
     * Imposta una data specifica nel tabellone (es: quando si viene reindirizzati dalla mappa bungalow).
     */
    fun initDate(m: Long) { if (m != -1L) { _currentDate.value = Calendar.getInstance().apply { timeInMillis = m }; loadData() } }


    /**
     * Avanza il tabellone al mese successivo e ricarica i dati occupazione.
     */
    fun nextMonth() { _currentDate.value = (_currentDate.value.clone() as Calendar).apply { add(Calendar.MONTH, 1) }; loadData() }


    /**
     * Arretra il tabellone al mese precedente e ricarica i dati occupazione.
     */
    fun previousMonth() { _currentDate.value = (_currentDate.value.clone() as Calendar).apply { add(Calendar.MONTH, -1) }; loadData() }

}