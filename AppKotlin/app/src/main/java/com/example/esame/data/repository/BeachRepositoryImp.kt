package com.example.esame.data.repository

import android.util.Log
import com.example.esame.data.model.Booking
import com.example.esame.data.model.Umbrella
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMPLEMENTAZIONE INTERFACCIA SPIAGGIA:
 * BeachRepositoryImpl è l'implementazione concreta dell'interfaccia BeachRepository.
 *
 * SCOPO: Gestire tutte le operazioni di persistenza dati riguardanti gli ombrelloni e le prenotazioni
 * interfacciandosi direttamente con Google Firebase Firestore.
 *
 * DESIGN CHOICES:
 * - @Inject: Utilizza Dagger Hilt per la Dependency Injection, permettendo di iniettare
 *   l'istanza di FirebaseFirestore configurata nel modulo Hilt.
 * - Coroutine (suspend functions): Tutte le operazioni di rete sono asincrone per non bloccare il Main Thread.
 * - .await(): Viene utilizzato l'estensione 'kotlinx-coroutines-play-services' per convertire
 *   i Task di Firebase in Coroutine, rendendo il codice sequenziale e più leggibile.
 */
@Singleton // Garantisce che esista una sola istanza del repository in tutta l'app
class BeachRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BeachRepository {

    private val umbrellaCollection: CollectionReference = firestore.collection("umbrella")
    private val bookingsCollection: CollectionReference = firestore.collection("bookings")

    companion object {
        private const val TAG = "BeachRepository"
    }

    /**
     * Ritorna una Query Firestore per gli ombrelloni ordinati per indice di riga.
     * Utilizzato solitamente per SnapshotListener (osservazione in tempo reale) nel ViewModel.
     */
    override fun getOmbrelloni(): Query =
        umbrellaCollection.orderBy("rowIndex", Query.Direction.ASCENDING)

    /**
     * Recupera la lista completa degli ombrelloni "una tantum".
     * Ordina i risultati prima per riga e poi per numero identificativo dell'ombrellone.
     *
     * @return List<Umbrella> o una lista vuota in caso di errore.
     */
    override suspend fun getSortedUmbrellasOnce(): List<Umbrella> {
        return try {
            val querySnapshot = umbrellaCollection
                .orderBy("rowIndex", Query.Direction.ASCENDING)
                .orderBy("number", Query.Direction.ASCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                // Converte il documento Firestore in oggetto Umbrella e associa l'ID del documento
                doc.toObject(Umbrella::class.java)?.apply { id = doc.id }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel recupero ordinato degli ombrelloni", e)
            emptyList()
        }
    }

    // Rimovibile non piu utilizzata, mantenuta per compatibilità con l'interfaccia.
    override suspend fun getBookingsForPeriod(startDate: Date, endDate: Date): List<Booking> {
        return emptyList()
    }

    /**
     * Aggiunge una nuova prenotazione al database.
     *
     * @param booking L'oggetto prenotazione da salvare.
     * @return Result con l'ID del documento creato o l'eccezione generata.
     */
    override suspend fun addBooking(booking: Booking): Result<String> {
        return try {
            val documentReference = bookingsCollection.add(booking).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiunta della prenotazione", e)
            Result.failure(e)
        }
    }

    /**
     * Recupera tutte le prenotazioni esistenti nel database.
     * Utilizzato per popolare lo stato globale delle occupazioni.
     */
    override suspend fun getAllBookings(): List<Booking> {
        return try {
            Log.d(TAG, "Inizio recupero totale prenotazioni...")
            val result = bookingsCollection.get().await().toObjects(Booking::class.java)
            Log.d(TAG, "Recuperate ${result.size} prenotazioni.")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento di tutte le prenotazioni", e)
            emptyList()
        }
    }

    /**
     * Elimina una prenotazione specifica tramite il suo ID documento.
     *
     * @param booking L'oggetto prenotazione che deve contenere un ID valido.
     */
    override suspend fun deleteBooking(booking: Booking): Result<Unit> {
        val docId = booking.id
        if (docId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Impossibile eliminare: ID mancante."))
        }

        return try {
            bookingsCollection.document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'eliminazione della prenotazione $docId", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica se un ombrellone è disponibile in un determinato intervallo di tempo.
     *
     * LOGICA DI CONFLITTO (Overlap):
     * Una prenotazione è in conflitto se:
     * 1. Una prenotazione esistente inizia durante il nuovo periodo.
     * 2. Una prenotazione esistente finisce durante il nuovo periodo.
     * 3. Una prenotazione esistente "contiene" interamente il nuovo periodo.
     *
     * @return true se disponibile, false se occupato o in caso di errore.
     */
    override suspend fun isUmbrellaAvailable(
        umbrellaId: String,
        newStartDate: Date,
        newEndDate: Date
    ): Boolean {
        return try {
            val startTimestamp = Timestamp(newStartDate)
            val endTimestamp = Timestamp(newEndDate)

            // Query 1: Conflitto per inizio (NewStart <= ExistStart < NewEnd)
            val conflictingStart = bookingsCollection
                .whereEqualTo("umbrellaId", umbrellaId)
                .whereGreaterThanOrEqualTo("startDate", startTimestamp)
                .whereLessThan("startDate", endTimestamp)
                .limit(1).get().await()

            if (!conflictingStart.isEmpty) return false

            // Query 2: Conflitto per fine (NewStart < ExistEnd <= NewEnd)
            val conflictingEnd = bookingsCollection
                .whereEqualTo("umbrellaId", umbrellaId)
                .whereGreaterThan("endDate", startTimestamp)
                .whereLessThanOrEqualTo("endDate", endTimestamp)
                .limit(1).get().await()

            if (!conflictingEnd.isEmpty) return false

            // Query 3: Conflitto per avvolgimento (ExistStart < NewStart AND ExistEnd > NewEnd)
            val conflictingWrap = bookingsCollection
                .whereEqualTo("umbrellaId", umbrellaId)
                .whereLessThan("startDate", startTimestamp)
                .whereGreaterThan("endDate", endTimestamp)
                .limit(1).get().await()

            if (!conflictingWrap.isEmpty) return false

            true // Nessun conflitto trovato
        } catch (e: Exception) {
            Log.e(TAG, "Errore controllo disponibilità Firestore", e)
            false // Politica conservativa: se c'è un errore, non permettiamo la prenotazione
        }
    }

    /**
     * Aggiorna una prenotazione esistente.
     *
     * DESIGN CHOICE: Viene utilizzato il metodo .set() che sovrascrive l'intero documento.
     * È una scelta sicura se l'oggetto 'booking' passato è completo.
     *
     * @param booking Oggetto Booking con i nuovi dati e ID corrispondente al documento Firestore.
     */
    override suspend fun updateBooking(booking: Booking): Result<Unit> {
        val docId = booking.id
        if (docId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("ID documento non valido per l'aggiornamento."))
        }

        return try {
            // Aggiorna l'intero documento con i nuovi campi dell'oggetto booking
            bookingsCollection.document(docId).set(booking).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'aggiornamento della prenotazione $docId", e)
            Result.failure(e)
        }
    }
}