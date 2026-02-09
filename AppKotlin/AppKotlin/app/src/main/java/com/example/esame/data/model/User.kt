package com.example.esame.data.model

/*
MODELLO UTENTE {
    Classe che rappresenta il modello dei dati di un utente
}
*/
data class User(
    val uid: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val role: String = ""
)
