package com.example.esame.data.repository
import com.example.esame.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


/*
REPOSITORY: GESTIONE AUTENTICAZIONE E UTENZA {
    Questa interfaccia definisce il contratto per tutte le operazioni di sicurezza.
    L'uso di un'interfaccia permette di iniettare diverse implementazioni (es: per i test)
    e separa la logica di business dalla libreria specifica (Firebase).
}
*/

interface AuthRepository {
    val currentUser: FirebaseUser?
    // Restituisce l'utente Firebase attualmente loggato (se presente)

    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    // Esegue l'accesso tramite credenziali classiche

    suspend fun createUser(user: User, password: String): Result<FirebaseUser>
    // Registra un nuovo account e inizializza il profilo su Firestore

    fun signOut()
    // Termina la sessione corrente

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    // Gestisce la procedura di recupero password tramite email

    fun getUserData(uid: String): Flow<User?>
    //Fornisce un flusso continuo di dati del profilo utente da Firestore

    fun getAuthStateFlow(): Flow<FirebaseUser?>
    // Monitora i cambiamenti nello stato di login/logout del sistema

}

/*
    IMPLEMENTAZIONE: Firebase Auth & Firestore
    Utilizza Hilt (@Inject) per ricevere le istanze di Firebase necessarie
*/
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

/*
MONITORAGGIO STATO AUTENTICAZIONE {
    Utilizza [callbackFlow] per trasformare il listener di Firebase in un Flow di Kotlin.
    È il cuore della reattività dell'app: permette alla UI di reagire immediatamente
    quando un utente entra o esce
}
*/
    override fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // Invia il nuovo utente (o null) nel flusso
            trySend(firebaseAuth.currentUser).isSuccess
        }
        auth.addAuthStateListener(authStateListener)

        /* awaitClose garantisce che il listener venga rimosso quando il Flow viene interrotto,
            prevenendo sprechi di memoria (memory leak) */
    awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

/*
LOGIN UTENTE {
    Usa le coroutine (.await()) per gestire l'operazione
    asincrona di Firebase in modo sequenziale.
}
*/
    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

/**
REGISTRAZIONE E CREAZIONE PROFILO {

Svolge tre compiti in sequenza {
    1) Crea l'account in Firebase Auth.
    2) Salva i metadati utente (nome, ruolo, etc.) su Firestore usando lo stesso UID.
    3) Invia l'email di verifica per sicurezza.
}
*/
    override suspend fun createUser(user: User, password: String): Result<FirebaseUser> {
        if (user.email.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("L'email non può essere vuota."))
        }
        return try {
            // Creazione account
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user!!

            // Salvataggio dati
            val userToSave = user.copy(uid = firebaseUser.uid)
            firestore.collection("users").document(firebaseUser.uid).set(userToSave).await()
            firebaseUser.sendEmailVerification().await()
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

// LOGOUT
    override fun signOut() {
        auth.signOut()
    }

// RESET PASSWORD
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

/*
STREAMING DATI UTENTE FIRESTORE {
    Osserva il documento dell'utente e aggiorna la UI ogni volta che
    un campo (es: cognome o ruolo) viene modificato sul database
}
*/
    override fun getUserData(uid: String): Flow<User?> = callbackFlow {
        val userDocument = firestore.collection("users").document(uid)
        val subscription = userDocument.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // Chiude il flow con un'eccezione se c'è un errore
                return@addSnapshotListener
            }
            // Trasforma il documento JSON in oggetto User
            val user = snapshot?.toObject(User::class.java)
            trySend(user).isSuccess
        }
    // Pulizia del listener
    awaitClose { subscription.remove() }
    }
}
