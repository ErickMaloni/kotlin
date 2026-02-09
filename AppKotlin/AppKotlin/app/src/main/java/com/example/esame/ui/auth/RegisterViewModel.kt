package com.example.esame.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.model.User
import com.example.esame.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rappresenta lo stato immutabile dell'interfaccia utente di registrazione.
 *
 * DESIGN CHOICE: L'uso di una data class per lo stato (UiState) centralizza tutte le variabili
 * reattive. Questo facilita il debug e garantisce che la UI rifletta sempre un unico
 * stato coerente dei dati.
 */
data class RegisterUiState(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: String = "-",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccess: Boolean = false
)

/**
 * ViewModel responsabile della gestione della logica di business per la creazione di nuovi account.
 *
 * SCOPO:
 * 1. Gestire l'inserimento dei dati utente tramite lo stato reattivo.
 * 2. Validare gli input prima di inviarli al database.
 * 3. Coordinare la comunicazione con l'[AuthRepository] per la creazione dell'utente su Firebase.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Versione mutabile privata dello stato per l'aggiornamento interno
    private val _uiState = MutableStateFlow(RegisterUiState())

    // Versione immutabile pubblica osservata dalla UI (Safe Access)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // FUNZIONI DI AGGIORNAMENTO STATO
    // Queste funzioni permettono alla UI di notificare ogni singolo cambiamento degli input

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onSurnameChange(surname: String) { _uiState.update { it.copy(surname = surname) } }
    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email) } }
    fun onPasswordChange(password: String) { _uiState.update { it.copy(password = password) } }
    fun onConfirmPasswordChange(confirm: String) { _uiState.update { it.copy(confirmPassword = confirm) } }
    fun onRoleChange(role: String) { _uiState.update { it.copy(role = role) } }

    /**
     * Resetta il messaggio di errore una volta che la UI lo ha visualizzato.
     */
    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Esegue la validazione della password secondo criteri di sicurezza standard.
     * @return true se la password soddisfa i requisiti (min 8 char, 1 maiuscola, 1 speciale).
     */
    private fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasUpperCase && hasSpecialChar
    }

    /**
     * Processa la richiesta di registrazione.
     *
     * LOGICA:
     * 1. Verifica che i campi obbligatori non siano vuoti.
     * 2. Confronta la password con la conferma password.
     * 3. Verifica i requisiti minimi di sicurezza della password.
     * 4. Avvia la coroutine per la persistenza su Firebase tramite il repository.
     */
    fun registerUser() {
        val state = uiState.value

        // Validazione Input
        if (state.name.isBlank() || state.surname.isBlank() || state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Tutti i campi sono obbligatori.") }
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Le password non coincidono.") }
            return
        }

        if (!isPasswordValid(state.password)) {
            _uiState.update { it.copy(errorMessage = "La password deve contenere almeno 8 caratteri, una maiuscola e un carattere speciale.") }
            return
        }

        // Avvio procedura asincrona
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Mappatura dei dati dallo stato UI al modello User di dominio
            val userToCreate = User(
                name = state.name,
                surname = state.surname,
                email = state.email,
                role = state.role
            )

            // Tentativo di creazione utente
            val result = repository.createUser(
                user = userToCreate,
                password = state.password
            )

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthException(exception)
                    )
                }
            }
        }
    }

    /**
     * Mappa le eccezioni tecniche di Firebase in stringhe leggibili per l'utente finale.
     *
     * SCELTA TECNICA: Si evita di mostrare errori tecnici grezzi (es. stacktrace)
     * per non confondere l'utente e proteggere i dettagli dell'infrastruttura.
     */
    private fun mapAuthException(e: Throwable): String {
        return when ((e as? FirebaseAuthException)?.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Questa email è già associata a un account esistente."
            "ERROR_INVALID_EMAIL" -> "Il formato dell'email non è corretto."
            "ERROR_WEAK_PASSWORD" -> "La password scelta è troppo debole."
            else -> "Errore durante la registrazione: ${e.localizedMessage}"
        }
    }
}