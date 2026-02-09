package com.example.esame.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esame.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rappresenta lo stato immutabile della UI per la schermata di Login.
 *
 * @property email Input dell'utente per l'indirizzo email.
 * @property password Input dell'utente per la password.
 * @property isLoading Indica se un'operazione asincrona (es. login) è in corso.
 * @property isAuthenticated Diventa true quando il login ha successo, innescando la navigazione.
 * @property errorMessage Contiene il messaggio di errore localizzato da mostrare all'utente.
 * @property passwordResetEmailSent Flag per mostrare un feedback dopo l'invio della mail di ripristino.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val passwordResetEmailSent: Boolean = false
)

/**
 * ViewModel responsabile della gestione della logica di autenticazione.
 *
 * DESIGN CHOICES:
 * - **StateFlow**: Utilizzato per gestire lo stato della UI in modo reattivo e thread-safe.
 * - **Dependency Injection (Hilt)**: L'istanza di [AuthRepository] viene iniettata per favorire il disaccoppiamento e la testabilità.
 * - **ViewModelScope**: Garantisce che tutte le coroutine vengano cancellate quando il ViewModel viene distrutto.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Stato interno mutabile (Privato)
    private val _uiState = MutableStateFlow(LoginUiState())

    // Stato esposto esternamente (Immutabile)
    val uiState = _uiState.asStateFlow()

    /**
     * Aggiorna l'email nello stato ad ogni cambiamento del campo di input.
     */
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    /**
     * Aggiorna la password nello stato ad ogni cambiamento del campo di input.
     */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    /**
     * Resetta il messaggio di errore dopo che è stato visualizzato (consumo dell'evento).
     */
    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Esegue il tentativo di accesso.
     *
     * LOGICA:
     * 1. Validazione locale (campi vuoti).
     * 2. Avvio dello stato di caricamento.
     * 3. Chiamata al repository.
     * 4. Gestione reattiva dell'esito (Successo -> Navigazione, Fallimento -> Mappatura errore).
     */
    fun signIn() {
        val currentEmail = uiState.value.email
        val currentPassword = uiState.value.password

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email e password non possono essere vuoti.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.signIn(currentEmail, currentPassword)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
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
     * Invia un'email di ripristino password utilizzando l'email attualmente inserita.
     */
    fun onForgotPasswordClick() {
        val currentEmail = uiState.value.email

        if (currentEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Inserisci la tua email per il ripristino.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.sendPasswordResetEmail(currentEmail)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, passwordResetEmailSent = true) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Errore nell'invio dell'email.") }
            }
        }
    }

    /**
     * Chiude il messaggio di conferma invio email di ripristino.
     */
    fun onPasswordResetEmailSentDismissed() {
        _uiState.update { it.copy(passwordResetEmailSent = false) }
    }

    /**
     * Mappa le eccezioni tecniche di Firebase in messaggi comprensibili per l'utente.
     *
     * SCELTA TECNICA: Si evita di mostrare errori tecnici (es. eccezioni stacktrace)
     * per migliorare l'esperienza utente e la sicurezza.
     */
    private fun mapAuthException(e: Throwable): String {
        return when ((e as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD" -> "Credenziali non valide."

            "ERROR_INVALID_EMAIL" -> "Il formato dell'email non è valido."

            else -> "Si è verificato un errore durante l'accesso. Riprova più tardi."
        }
    }
}