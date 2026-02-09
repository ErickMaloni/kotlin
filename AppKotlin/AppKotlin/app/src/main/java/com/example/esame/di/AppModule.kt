package com.example.esame.di

import com.example.esame.data.repository.AuthRepository
import com.example.esame.data.repository.AuthRepositoryImpl
import com.example.esame.data.repository.BeachRepository
import com.example.esame.data.repository.BeachRepositoryImpl
import com.example.esame.data.repository.PriceRepository
import com.example.esame.data.repository.PriceRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule è il modulo principale per la Dependency Injection tramite Dagger Hilt.
 *
 * DESIGN CHOICES:
 * - @Module: Indica che questa classe fornisce istanze di oggetti (dipendenze).
 * - @InstallIn(SingletonComponent::class): Specifica che le dipendenze qui create
 *   vivranno per tutta la durata dell'applicazione (scope globale).
 * - @Provides: Utilizzato perché Firebase e le interfacce non possono essere istanziate
 *   direttamente con il costruttore @Inject.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fornisce l'istanza singleton di FirebaseAuth.
     * Scelta: Usiamo il Singleton per evitare di inizializzare l'SDK di Firebase più volte.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Fornisce l'istanza singleton di FirebaseFirestore.
     * Scelta: Centralizzare Firestore permette di gestire meglio le impostazioni (come la cache)
     * in un unico punto se necessario in futuro.
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    /**
     * Collega l'interfaccia AuthRepository alla sua implementazione concreta AuthRepositoryImpl.
     *
     * @param auth Richiede FirebaseAuth (fornito dalla funzione sopra).
     * @param firestore Richiede FirebaseFirestore (fornito dalla funzione sopra).
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore)
    }

    /**
     * Collega l'interfaccia PriceRepository alla sua implementazione PriceRepositoryImpl.
     * Hilt inietterà automaticamente questo repository nei ViewModel che lo richiedono.
     */
    @Provides
    @Singleton
    fun providePriceRepository(firestore: FirebaseFirestore): PriceRepository {
        return PriceRepositoryImpl(firestore)
    }

    /**
     * Collega l'interfaccia BeachRepository alla sua implementazione BeachRepositoryImpl.
     *
     * PERCHÉ QUESTA FUNZIONE È IMPORTANTE:
     * Nelle altre classi (es. ViewModel) noi chiederemo un "BeachRepository" (astrazione).
     * Questa funzione dice a Hilt: "Ogni volta che qualcuno vuole un BeachRepository,
     * crea e consegna un BeachRepositoryImpl".
     * Questo segue il principio SOLID di Dependency Inversion.
     */
    @Provides
    @Singleton
    fun provideBeachRepository(firestore: FirebaseFirestore): BeachRepository {
        return BeachRepositoryImpl(firestore)
    }
}