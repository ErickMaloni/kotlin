package com.example.esame.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/*
MODELLO PRENOTAZIONE OMBRELLONI {
    Classe che rappresenta una singola prenotazione.
    L'annotazione @DocumentId dice a Firestore di popolare questo campo
    con l'ID del documento quando legge i dati.
}
*/

enum class BookingStatus {
    CONFIRMED, // Prenotazione attiva
    PENDING,   // In attesa di riassegnazione
}

data class Booking(

    @DocumentId val id: String? = null,

    // Campi della prenotazione
    var umbrellaId: String? = null,
    var clientName: String? = null,
    var clientSurname: String? = null,
    var startDate: Timestamp? = null,
    var endDate: Timestamp? = null,
    var seasonal: Boolean = false,
    var totalPrice: Double = 0.0,
    var status: BookingStatus = BookingStatus.CONFIRMED
) {
    // Costruttore senza argomenti, richiesto da Firestore per la deserializzazione.
    constructor() : this(
        id = null,
        umbrellaId = null,
        clientName = null,
        clientSurname = null,
        startDate = null,
        endDate = null,
        seasonal = false,
        totalPrice = 0.0,
        status = BookingStatus.CONFIRMED
    )
}
