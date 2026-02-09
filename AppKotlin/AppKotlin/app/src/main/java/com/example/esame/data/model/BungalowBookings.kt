package com.example.esame.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId


/*
MODELLO DATI: PRENOTAZIONE BUNGALOW {
    Questa classe rappresenta una singola prenotazione all'interno della collezione "bookingsBungalow".
    Gestisce tutte le informazioni relative al soggiorno del cliente, i costi aggiuntivi e
    l'eventuale collegamento con una prenotazione spiaggia.
}
 */
data class BungalowBooking(
    @DocumentId val bookingId: String? = null,

    var bungalowId: String? = null,
    var clientName: String? = null,
    var clientSurname: String? = null,
    var startDate: Timestamp? = null,
    var endDate: Timestamp? = null,
    var numPeople: Int = 1,
    var hasPets: Boolean = false,
    var cleaningFee: Double = 15.0,
    var petFee: Double = 0.0,
    var totalPrice: Double = 0.0,
    var status: String = "LIBERO",
    val isSeasonal: Boolean = false,
    var linkedUmbrellaBookingId: String? = null
) {
    // Costruttore vuoto aggiornato
    constructor() : this(
        bookingId = null,
        bungalowId = null,
        clientName = null,
        clientSurname = null,
        startDate = null,
        endDate = null,
        numPeople = 1,
        hasPets = false,
        cleaningFee = 15.0,
        petFee = 0.0,
        totalPrice = 0.0,
        status = "LIBERO",
        isSeasonal = false,
        linkedUmbrellaBookingId = null
    )
}