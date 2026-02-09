package com.example.esame.data.repository

import android.util.Log
import com.example.esame.data.model.Price
import com.example.esame.data.model.PriceList
import com.example.esame.data.model.SubscriptionPrice
import com.example.esame.data.model.BungalowPriceList
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMPLEMENTAZIONE REPOSITORY LISTINO PREZZI:
 *
 * DESIGN CHOICES:
 * - **CallbackFlow**: Utilizzato per trasformare i listener in tempo reale di Firebase in Flow di Kotlin.
 *   Questo permette di osservare le modifiche ai prezzi istantaneamente nella UI.
 * - **Write Batch**: Utilizzato nelle funzioni di update per garantire l'atomicità: o tutti i prezzi
 *   vengono aggiornati con successo, o nessuno (evitando stati inconsistenti).
 * - **Polimorfismo Firestore**: Gestisce diversi tipi di modelli (PriceList vs SubscriptionPrice)
 *   basandosi sul campo "period".
 */
@Singleton
class PriceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PriceRepository {

    // Riferimenti alle collezioni Firestore
    private val priceCollection: CollectionReference = firestore.collection("price")
    private val bungalowPriceCollection: CollectionReference = firestore.collection("priceBungalow")

    companion object {
        private const val BEACH_TAG = "PRICE_REPO_BEACH"
        private const val BUNGALOW_TAG = "PRICE_REPO_BUNGALOW"
    }

    // GESTIONE PREZZI SPIAGGIA

    /**
     * Recupera il listino prezzi della spiaggia in tempo reale.
     *
     * @return Flow contenente una lista di oggetti [Price].
     *
     * Nota: La funzione distingue tra prezzi standard e abbonamenti stagionali
     * mappando i documenti in diverse classi ([SubscriptionPrice] o [PriceList]).
     */
    override fun getPrices(): Flow<List<Price>> = callbackFlow {
        val query = priceCollection.orderBy("period", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(BEACH_TAG, "Errore durante l'ascolto dei prezzi spiaggia", error)
                close(error)
                return@addSnapshotListener
            }

            val pricesList = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val period = doc.getString("period")
                    // Logica di mapping polimorfico basata sul contenuto del documento
                    val priceObject: Price? = when (period) {
                        "SEASONAL" -> doc.toObject(SubscriptionPrice::class.java)
                        else -> doc.toObject(PriceList::class.java)
                    }
                    priceObject?.apply { id = doc.id }
                } catch (e: Exception) {
                    Log.e(BEACH_TAG, "Errore conversione documento spiaggia: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            trySend(pricesList).isSuccess
        }

        // Importante: Rimuove il listener di Firebase quando il Flow non è più osservato
        awaitClose { listener.remove() }
    }

    /**
     * Aggiorna massivamente i prezzi della spiaggia.
     *
     * @param prices Lista dei prezzi da aggiornare.
     * @return Result.success se l'operazione batch è riuscita.
     */
    override suspend fun updatePrices(prices: List<Price>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            prices.forEach { price ->
                if (price.id.isNotBlank()) {
                    val docRef = priceCollection.document(price.id)
                    batch.set(docRef, price)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(BEACH_TAG, "Errore durante l'aggiornamento batch spiaggia", e)
            Result.failure(e)
        }
    }

    // GESTIONE PREZZI BUNGALOW

    /**
     * Recupera il listino prezzi dei bungalow in tempo reale.
     *
     * @return Flow di liste di [BungalowPriceList].
     *
     * Scelta tecnica: Include log di debug dettagliati per monitorare la
     * corrispondenza tra i nomi dei campi su Firestore e le proprietà della classe Model.
     */
    override fun getBungalowPrices(): Flow<List<BungalowPriceList>> = callbackFlow {
        val query = bungalowPriceCollection.orderBy("period", Query.Direction.ASCENDING)

        Log.d(BUNGALOW_TAG, "Avvio osservazione collezione bungalow...")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(BUNGALOW_TAG, "Errore critico durante l'ascolto bungalow", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                Log.w(BUNGALOW_TAG, "Nessun documento trovato nella collezione 'priceBungalow'")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val bungalowObj = doc.toObject(BungalowPriceList::class.java)
                    if (bungalowObj == null) {
                        Log.e(BUNGALOW_TAG, "Deserializzazione fallita per il documento: ${doc.id}")
                    } else {
                        bungalowObj.id = doc.id
                        Log.v(BUNGALOW_TAG, "Documento caricato: ${bungalowObj.period}")
                    }
                    bungalowObj
                } catch (e: Exception) {
                    Log.e(BUNGALOW_TAG, "Errore di mapping per ${doc.id}: ${e.message}", e)
                    null
                }
            }

            Log.d(BUNGALOW_TAG, "Inviati ${list.size} listini bungalow alla UI")
            trySend(list).isSuccess
        }

        awaitClose {
            Log.d(BUNGALOW_TAG, "Chiusura listener bungalow")
            listener.remove()
        }
    }

    /**
     * Aggiorna massivamente i prezzi dei bungalow utilizzando un'operazione Batch.
     *
     * @param prices Lista dei listini bungalow da salvare.
     */
    override suspend fun updateBungalowPrices(prices: List<BungalowPriceList>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            prices.forEach { bungalowPrice ->
                if (bungalowPrice.id.isNotBlank()) {
                    val docRef = bungalowPriceCollection.document(bungalowPrice.id)
                    batch.set(docRef, bungalowPrice)
                }
            }
            batch.commit().await()
            Log.i(BUNGALOW_TAG, "Aggiornamento batch bungalow completato con successo")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(BUNGALOW_TAG, "Errore durante il salvataggio dei prezzi bungalow", e)
            Result.failure(e)
        }
    }
}