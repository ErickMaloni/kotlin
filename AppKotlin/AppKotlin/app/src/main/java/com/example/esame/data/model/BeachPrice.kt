package com.example.esame.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/*
STRUTTURA PREZZI SPIAGGIA {
    Questa classe sealed gestisce il polimorfismo dei prezzi su Firestore.
 }
*/

//ID univoco del documento Firestore
sealed class Price {
    @get:DocumentId
    abstract var id: String
}


/*
MODELLO LISTINO PREZZI PERIODICO (Mese/Settimana) {
    Rappresenta documenti come "GIUGNO" o "LUGLIO".
    Invece di un prezzo singolo, contiene una mappa di [Week] (settimane),
    permettendo di variare il costo dell'ombrellone in base al periodo specifico del mese.
}
*/
data class PriceList(
    @DocumentId override var id: String = "",

    val period: String? = null,
    val startRow: Long? = null,
    val endRow: Long? = null,

    val notes: String? = null,

    /** Mappa delle settimane: Chiave = Nome Settimana, Valore = Oggetto Week con date e prezzi */
    val weeks: Map<String, Week>? = null
) : Price() {
    /** Costruttore vuoto necessario per la deserializzazione automatica di Firebase */
    constructor() : this("", null, null, null, null, null)
}

/*
MODELLO ABBONAMENTO STAGIONALE {
    Rappresenta un prezzo standard per l'intera stagione (documento "SEASONAL").
    A differenza del listino periodico, qui il prezzo è unico e definito a livello principale.
}
*/
data class SubscriptionPrice(
    @DocumentId override var id: String = "",

    /** Identificativo del periodo (es: "SEASONAL") */
    val period: String? = null,

    /** Range di file coperte dall'abbonamento */
    val startRow: Long? = null,
    val endRow: Long? = null,

    /** Prezzo totale dell'abbonamento */
    val price: Long? = null,

    /** Date di inizio e fine validità dell'abbonamento stagionale */
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
) : Price() {
    /** Costruttore vuoto richiesto da Firestore */
    constructor() : this("", null, null, null, null, null, null)
}

/**
 * MODELLO SETTIMANA (Dati nidificati)
 *
 * Rappresenta i dettagli economici e temporali di una singola settimana
 * all'interno di un documento [PriceList].
 */
data class Week(

    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val weekdayPrice: Long? = null,
    val holidayPrice: Long? = null

) {
    constructor() : this(null, null, null, null)
}