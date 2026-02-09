package com.example.esame.ui.price

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.Price
import com.example.esame.data.model.PriceList
import com.example.esame.data.model.SubscriptionPrice
import com.example.esame.data.model.Week
import com.example.esame.data.model.BungalowPriceList
import com.example.esame.data.repository.PriceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Classe wrapper per gestire eventi "one-shot" (es. messaggi Toast).
 *
 * DESIGN CHOICE: Questo pattern previene che lo stesso evento (es. "Salvataggio riuscito")
 * venga mostrato più volte in caso di ricomposizione della UI (es. rotazione schermo).
 */
data class Event<T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else { hasBeenHandled = true; content }
    }
}

/**
 * Definisce i possibili stati dell'interfaccia utente per la gestione dei listini prezzi.
 *
 * DESIGN CHOICE: L'uso di una `sealed interface` permette di rappresentare in modo sicuro
 * e prevedibile tutti gli stati possibili della UI (Caricamento, Successo, Errore).
 */
sealed interface PriceListUiState {
    data class Success(
        val beachPrices: List<Price> = emptyList(),
        val bungalowPrices: List<BungalowPriceList> = emptyList(),
        val selectedTab: Int = 0,
        val selectedBungalowMonth: String = "TUTTI",
        val selectedBeachFilter: String = "TUTTI", // AGGIUNTO: Filtro spiaggia
        val isSaving: Boolean = false,
        val userMessage: Event<String>? = null
    ) : PriceListUiState

    object Error : PriceListUiState
    object Loading : PriceListUiState
}

/**
 * ViewModel responsabile della logica di business per la visualizzazione e modifica dei listini prezzi.
 *
 * SCOPO:
 * 1. Caricare e osservare i prezzi da [PriceRepository] in modo reattivo.
 * 2. Gestire l'input dell'utente (cambio tab, filtri, modifica prezzi).
 * 3. Coordinare le operazioni di salvataggio sul database.
 *
 * SCELTE ARCHITETTURALI:
 * - **Hilt**: Utilizzato per l'iniezione delle dipendenze, disaccoppiando il ViewModel dal Repository.
 * - **Combine Flow**: Unisce più flussi di dati (prezzi spiaggia, prezzi bungalow, filtri) in un unico
 *   flusso di stato, garantendo che la UI si aggiorni reattivamente a qualsiasi cambiamento.
 */
@HiltViewModel
class PriceListViewModel @Inject constructor(
    private val priceRepository: PriceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PriceListUiState>(PriceListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    private val _selectedBungalowMonth = MutableStateFlow("TUTTI")
    private val _selectedBeachFilter = MutableStateFlow("TUTTI") // AGGIUNTO

    init {
        loadAndObserveAllPrices()
    }

    /**
     * Inizializza l'osservazione dei dati.
     * Utilizza `combine` per unire i flussi reattivi dei prezzi e dei filtri.
     * Se uno qualsiasi di questi flussi emette un nuovo valore, l'intero blocco viene
     * rieseguito per produrre un nuovo stato UI aggiornato.
     */
    private fun loadAndObserveAllPrices() {
        viewModelScope.launch {
            Log.d("DEBUG_FLOW", "ViewModel: Avvio osservazione flussi...")
            combine(
                priceRepository.getPrices().catch { emit(emptyList()) },
                priceRepository.getBungalowPrices().catch { emit(emptyList()) },
                _selectedTab,
                _selectedBungalowMonth,
                _selectedBeachFilter // AGGIUNTO al combine
            ) { beach, bungalow, tab, bMonth, beachFilter ->

                Log.d("DEBUG_FLOW", "Dati uniti - Spiaggia: ${beach.size}, Bungalow: ${bungalow.size}, Tab: $tab")

                PriceListUiState.Success(
                    beachPrices = beach,
                    bungalowPrices = bungalow,
                    selectedTab = tab,
                    selectedBungalowMonth = bMonth,
                    selectedBeachFilter = beachFilter, // Passato allo stato
                    isSaving = (_uiState.value as? PriceListUiState.Success)?.isSaving ?: false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // AZIONI UTENTE
    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun onBungalowMonthSelected(month: String) {
        _selectedBungalowMonth.value = month
    }

    fun onBeachFilterSelected(filter: String) {
        _selectedBeachFilter.value = filter
    }

    // LOGICA SPIAGGIA

    /**
     * Aggiorna localmente i prezzi settimanali (feriale/festivo) di un listino spiaggia.
     */
    fun onWeeklyPriceChange(priceListId: String, weekKey: String, newWeekdayPrice: String, newHolidayPrice: String) {
        _uiState.update { currentState ->
            if (currentState !is PriceListUiState.Success) return@update currentState

            val updated = currentState.beachPrices.map { price ->
                if (price is PriceList && price.id == priceListId) {
                    val currentWeeks = price.weeks ?: emptyMap()
                    val weekToUpdate = currentWeeks[weekKey] ?: Week()
                    val updatedWeek = weekToUpdate.copy(
                        weekdayPrice = newWeekdayPrice.toLongOrNull() ?: 0L,
                        holidayPrice = newHolidayPrice.toLongOrNull() ?: 0L
                    )
                    val updatedWeeksMap = currentWeeks.toMutableMap().apply { this[weekKey] = updatedWeek }
                    price.copy(weeks = updatedWeeksMap)
                } else price
            }
            currentState.copy(beachPrices = updated)
        }
    }


    /**
     * Aggiorna localmente il prezzo di un abbonamento stagionale.
     */
    fun onSubscriptionPriceChange(subscriptionId: String, newPrice: String) {
        _uiState.update { currentState ->
            if (currentState !is PriceListUiState.Success) return@update currentState
            val updated = currentState.beachPrices.map { price ->
                if (price is SubscriptionPrice && price.id == subscriptionId) {
                    price.copy(price = newPrice.toLongOrNull() ?: 0L)
                } else price
            }
            currentState.copy(beachPrices = updated)
        }
    }

    // LOGICA BUNGALOW

    /**
     * Aggiorna localmente il prezzo di una specifica tipologia di bungalow (standard, deluxe)
     * per una determinata settimana.
     */
    fun onBungalowPriceChange(
        priceListId: String,
        weekKey: String,
        typeKey: String,
        newPrice: String
    ) {
        _uiState.update { currentState ->
            if (currentState !is PriceListUiState.Success) return@update currentState

            val updatedBungalows = currentState.bungalowPrices.map { list ->
                if (list.id == priceListId) {
                    val updatedWeeks = list.weeks?.toMutableMap() ?: mutableMapOf()
                    val weekToUpdate = updatedWeeks[weekKey] ?: return@map list

                    val updatedPrices = weekToUpdate.price.toMutableMap().apply {
                        this[typeKey] = newPrice.toDoubleOrNull() ?: 0.0
                    }

                    updatedWeeks[weekKey] = weekToUpdate.copy(price = updatedPrices)
                    list.copy(weeks = updatedWeeks)
                } else list
            }
            currentState.copy(bungalowPrices = updatedBungalows)
        }
    }


    /**
     * Aggiorna localmente il prezzo stagionale di una specifica tipologia di bungalow.
     */
    fun onBungalowSeasonalPriceChange(priceListId: String, typeKey: String, newPrice: String) {
        _uiState.update { currentState ->
            if (currentState !is PriceListUiState.Success) return@update currentState

            val updated = currentState.bungalowPrices.map { list ->
                if (list.id == priceListId) {
                    val updatedPrices = list.price?.toMutableMap() ?: mutableMapOf()
                    updatedPrices[typeKey] = newPrice.toDoubleOrNull() ?: 0.0
                    list.copy(price = updatedPrices)
                } else list
            }
            currentState.copy(bungalowPrices = updated)
        }
    }

    // SALVATAGGIO

    /**
     * Esegue il salvataggio dei listini prezzi modificati (spiaggia o bungalow)
     * in base al tab attualmente selezionato.
     */
    fun onSaveClick() {
        val currentState = _uiState.value as? PriceListUiState.Success ?: return
        if (currentState.isSaving) return

        viewModelScope.launch {
            _uiState.update { currentState.copy(isSaving = true) }

            // Determina quale repository chiamare in base al tab attivo
            val result = if (currentState.selectedTab == 0) {
                priceRepository.updatePrices(currentState.beachPrices)
            } else {
                priceRepository.updateBungalowPrices(currentState.bungalowPrices)
            }

            val message = if (result.isSuccess) {
                "Modifiche salvate con successo!"
            } else {
                Log.e("VIEWMODEL_SAVE", "Errore salvataggio", result.exceptionOrNull())
                "Errore durante il salvataggio."
            }

            _uiState.update {
                if (it is PriceListUiState.Success) {
                    it.copy(isSaving = false, userMessage = Event(message))
                } else it
            }
        }
    }
}