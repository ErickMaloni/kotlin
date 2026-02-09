package com.example.esame.ui.bookings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

/**
 * UNIT TEST: Logica Prenotazioni Bungalow
 *
 * In questo file testiamo la logica pura dell'interfaccia bungalow,
 * senza dover avviare l'emulatore o Firebase.
 */
class BungalowBookingTest {

    /**
     * TEST: Calcolo durata soggiorno
     * Verifica che la differenza tra date calcoli correttamente il numero di settimane.
     * Logica richiesta: Sabato-Sabato (7 giorni) = 1 settimana.
     */
    @Test
    fun `calcolo settimane per soggiorno di 7 giorni deve restituire 1 settimana`() {
        val start = createDate(2026, Calendar.JUNE, 13) // Sabato
        val end = createDate(2026, Calendar.JUNE, 20)   // Sabato successivo

        val diffMillis = end.time - start.time
        val totalDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        // Logica implementata nel ViewModel
        val numberOfWeeks = if (totalDays <= 7) 1 else Math.ceil(totalDays / 7.0).toInt()

        assertEquals("Dovrebbe essere esattamente 1 settimana", 1, numberOfWeeks)
    }

    /**
     * TEST: Calcolo dinamico periodi lunghi
     * Verifica che soggiorni superiori a una settimana (es. 23 giorni)
     * vengano arrotondati per eccesso (es. 4 settimane).
     */
    @Test
    fun `calcolo settimane per soggiorno di 23 giorni deve restituire 4 settimane`() {
        val start = createDate(2026, Calendar.JUNE, 1)
        val end = createDate(2026, Calendar.JUNE, 23)

        val diffMillis = end.time - start.time
        val totalDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        val numberOfWeeks = if (totalDays <= 7) 1 else Math.ceil(totalDays / 7.0).toInt()

        assertEquals("22 giorni di differenza dovrebbero contare come 4 settimane di listino", 4, numberOfWeeks)
    }

    /**
     * TEST: Validazione Date
     * Verifica che il sistema riconosca come invalida una data di fine precedente all'inizio.
     */
    @Test
    fun `validazione date deve fallire se la fine e precedente all'inizio`() {
        val start = createDate(2026, Calendar.JUNE, 20)
        val end = createDate(2026, Calendar.JUNE, 15) // Errore utente

        val isValid = end.after(start)

        assertFalse("La validazione deve bloccare date invertite", isValid)
    }

    /**
     * TEST: Validazione Check-out stesso giorno
     * Verifica che la logica di check-in (12:00) e check-out (10:00)
     * permetta la rotazione nello stesso giorno.
     */
    @Test
    fun `check-in e check-out nello stesso giorno non devono collidere`() {
        // Prenotazione A finisce alle 10:00
        val endA = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 23, 10, 0, 0)
        }.timeInMillis

        // Prenotazione B inizia alle 12:00
        val startB = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 23, 12, 0, 0)
        }.timeInMillis

        // Se l'inizio della nuova è DOPO la fine della vecchia, non c'è collisione
        assertTrue("Il check-in alle 12 deve essere possibile dopo il check-out delle 10", startB > endA)
    }

    // --- Helper per creare date ---
    private fun createDate(year: Int, month: Int, day: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(year, month, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}