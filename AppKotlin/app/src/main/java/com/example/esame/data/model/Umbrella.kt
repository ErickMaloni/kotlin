package com.example.esame.data.model

import com.google.firebase.firestore.Exclude

/*
MODELLO OMBRELLONE {
    Rappresenta un singolo ombrellone.
    L'ID del documento Firestore viene salvato nella variabile 'id' dopo la lettura.
    L'annotazione @get:Exclude previene che il campo 'id' venga riscritto su Firestore.
}
*/

data class Umbrella(
    @get:Exclude var id: String = "",

/*
    Usiamo il tipo Long (invece di Int) principalmente per
    garantire la compatibilità diretta con Firebase Firestore
*/
    val number: Long = 0,
    val rowIndex: Long = 0
    )
