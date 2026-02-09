package com.example.esame.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/*
MODELLO LISTINO PREZZI BUNGALOW {
    Questa classe mappa i documenti della collezione "priceBungalow" su Firestore.
    È progettata per gestire due tipi di configurazioni:
    Listini Mensili: Documenti come "JUNE", "JULY" che contengono prezzi settimanali.
    Listini Stagionali: Documenti come "SEASONAL" che contengono un prezzo unico.
}
*/

data class BungalowPriceList(
    @DocumentId var id: String = "",
    val period: String = "",

    /*
        Mappa delle settimane per i listini periodici.
        Chiave: Identificativo della settimana (es: "week_1").
        Valore: Oggetto [BungalowWeekDetails] con date e prezzi specifici.
     */
    @get:PropertyName("weeks")
    @set:PropertyName("weeks")
    var weeks: Map<String, BungalowWeekDetails>? = null,

    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Map<String, Any>? = null,

    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
) {
    // Costruttore vuoto necessario per Firebase
    constructor() : this("", "", null, null, null, null)
}

data class BungalowWeekDetails(
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,

    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Map<String, Any> = emptyMap()
) {
    // Costruttore vuoto necessario per Firebase
    constructor() : this(null, null, emptyMap())
}