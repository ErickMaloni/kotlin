package com.example.esame.data.repository

import com.example.esame.data.model.Price
import com.example.esame.data.model.BungalowPriceList // Assicurati di aver creato questo modello
import kotlinx.coroutines.flow.Flow

interface PriceRepository {

    //  GESTIONE PREZZI ATTUALI
    fun getPrices(): Flow<List<Price>>
    fun getBungalowPrices(): Flow<List<BungalowPriceList>>


    //  GESTIONE PREZZI AGGIORNATI
    suspend fun updatePrices(prices: List<Price>): Result<Unit>
    suspend fun updateBungalowPrices(bungalowPrices: List<BungalowPriceList>): Result<Unit>
}