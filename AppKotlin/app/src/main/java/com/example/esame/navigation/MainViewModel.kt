package com.example.esame.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.User
import com.example.esame.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Rappresenta lo stato della UI per la navigazione principale e il controllo accessi.
 *
 * @property isLoading Indica se è in corso il recupero iniziale dei dati utente.
 * @property user L'oggetto [User] contenente i dettagli del profilo (ruolo, nome, etc.),
 *                null se l'utente non è autenticato.
 */
data class MainUiState(
    val isLoading: Boolean = true,
    val user: User? = null
)

/**
 * MainViewModel è il "cuore pulsante" della navigazione dell'intera applicazione.
 *
 * SCOPO:
 * Gestire lo stato di autenticazione globale in modo reattivo. Decide se l'utente deve
 * vedere la schermata di Login o la Dashboard in base alla sessione Firebase attiva.
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow (UDF)**: Espone un unico [StateFlow] per gestire lo stato della UI.
 * - **Reattività (flatMapLatest)**: Il ViewModel reagisce istantaneamente ai cambiamenti di stato
 *   di Firebase (Login/Logout) senza bisogno di chiamate manuali.
 * - **Dependency Injection**: Utilizza [AuthRepository] iniettato tramite Hilt per separare
 *   la logica dei dati dalla logica della UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * uiState è l'unica fonte di verità per la navigazione.
     *
     * LOGICA DI FUNZIONAMENTO:
     * 1. Ascolta il flusso di autenticazione di Firebase ([getAuthStateFlow]).
     * 2. Se l'utente è nullo, imposta lo stato come non autenticato.
     * 3. Se l'utente esiste, concatena una seconda richiesta ([getUserData]) per recuperare
     *    il ruolo e i dettagli dal database Firestore.
     *
     * PERCHÉ [flatMapLatest]:
     * Garantisce che se l'utente cambia (es. fa logout veloce e login con altro account),
     * le vecchie sottoscrizioni ai dati Firestore vengano cancellate immediatamente,
     * prevenendo memory leak o visualizzazione di dati errati.
     *
     * PERCHÉ [stateIn]:
     * Converte un Flow "freddo" in uno Stato "caldo". [SharingStarted.WhileSubscribed(5000)]
     * permette di mantenere lo stato vivo per 5 secondi dopo che la UI scompare (es. rotazione schermo),
     * ottimizzando le performance ed evitando ricaricamenti inutili.
     */
    val uiState: StateFlow<MainUiState> = authRepository.getAuthStateFlow()
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(MainUiState(isLoading = false, user = null))
            } else {
                authRepository.getUserData(firebaseUser.uid).map { userFromFirestore ->
                    MainUiState(isLoading = false, user = userFromFirestore)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(isLoading = true)
        )

    /**
     * Gestisce la richiesta di disconnessione dell'utente.
     *
     * NOTA: Non è necessario aggiornare manualmente lo stato qui. Chiamando [signOut],
     * il flusso [authRepository.getAuthStateFlow()] emetterà automaticamente un valore null,
     * scatenando l'aggiornamento reattivo di [uiState] e reindirizzando l'utente al Login.
     */
    fun onLogoutClick() {
        authRepository.signOut()
    }
}