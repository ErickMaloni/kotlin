package com.example.esame.data.model

import com.google.firebase.firestore.Exclude

/**
 * Rappresenta un singolo bungalow.
 * Coerente con il modello Umbrella.
 */
data class Bungalow(
    @get:Exclude var id: String = "",
    val number: Long = 0,
    val type: String = "",
    val capacity: Long = 0,
    val status: String = "LIBERO"
) {
    // Costruttore vuoto necessario per Firebase
    constructor() : this("", 0, "", 0, "LIBERO")
}