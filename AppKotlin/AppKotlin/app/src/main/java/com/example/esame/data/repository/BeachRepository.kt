package com.example.esame.data.repository

import com.example.esame.data.model.Booking
import com.example.esame.data.model.Umbrella
import com.google.firebase.firestore.Query
import java.util.Date

/**
 * REPOSITORY INTERFACE: GESTIONE SPIAGGIA
 *
 * Questa interfaccia definisce il contratto per tutte le operazioni relative
 * alla spiaggia (ombrelloni e relative prenotazioni)
 * Segue il principio di inversione delle dipendenze, permettendo di separare
 * la logica della UI dall'implementazione specifica di Firebase
*/
interface BeachRepository {

// GESTIONE STRUTTURA [ OMBRELLONI ]

/**
  * Fornisce una Query Firestore per la collezione degli ombrelloni.
  * Utilizzata principalmente per i listener in tempo reale (SnapshotListener)
  * per aggiornare la mappa della spiaggia istantaneamente.
*/
    fun getOmbrelloni(): Query

/**
  * Recupera una lista statica di tutti gli ombrelloni, ordinandoli per indice di fila.
  * Viene usata nelle fasi di inizializzazione o nei calcoli del ViewModel
  * che richiedono una lista completa e ordinata una tantum.
*/
    suspend fun getSortedUmbrellasOnce(): List<Umbrella>

// GESTIONE PRENOTAZIONI (LETTURA)

/**
  * Recupera l'elenco completo di tutte le prenotazioni presenti nel database.
  * Necessaria per popolare la cache locale o per funzioni di ricerca globale.
*/
    suspend fun getAllBookings(): List<Booking>

/**
  * @deprecated Questa funzione è stata sostituita da metodi più granulari
  * per distinguere tra listini stagionali e periodici.
*/
    @Deprecated(
        "Sostituita da metodi più specifici per ottimizzare le query Firebase.",
        replaceWith = ReplaceWith("getNonSeasonalBookingsForPeriod e getSeasonalBookings")
    )
    suspend fun getBookingsForPeriod(startDate: Date, endDate: Date): List<Booking>


// OPERAZIONI DI SCRITTURA (CRUD)

/**
  * Registra una nuova prenotazione su Firestore.
  * @param booking L'oggetto prenotazione da salvare.
  * @return [Result] contenente l'ID generato da Firebase in caso di successo.
*/
    suspend fun addBooking(booking: Booking): Result<String>

/**
  * Aggiorna i dati di una prenotazione esistente.
  * Viene invocata durante la modifica di date, nomi o al cambio di ombrellone.
*/
    suspend fun updateBooking(booking: Booking): Result<Unit>


// Rimuove definitivamente una prenotazione dal database.
    suspend fun deleteBooking(booking: Booking): Result<Unit>


//  LOGICA DI VALIDAZIONE E SICUREZZA
/**
  * Verifica la disponibilità di un ombrellone in un intervallo temporale.
  * @param umbrellaId L'ID dell'ombrellone da controllare.
  * @param newStartDate Data inizio della nuova prenotazione.
  * @param newEndDate Data fine della nuova prenotazione.
  * @return true se l'ombrellone è libero, false se esiste una sovrapposizione.
  * Perché è necessaria: Previene il fenomeno dell'overbooking, garantendo
  * che lo stesso posto non venga venduto a due clienti diversi nello stesso periodo.
*/
    suspend fun isUmbrellaAvailable(umbrellaId: String, newStartDate: Date, newEndDate: Date): Boolean
}
