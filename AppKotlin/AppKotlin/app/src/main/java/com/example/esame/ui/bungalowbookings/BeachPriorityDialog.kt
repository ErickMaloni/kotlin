package com.example.esame.ui.bungalowbookings // Assicurati che il package sia corretto

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


/**
 * BeachPriorityDialog è un componente UI di tipo Dialog (finestra modale).
 *
 * SCOPO:
 * Questo dialogo viene mostrato immediatamente dopo il salvataggio di una prenotazione
 * per un bungalow. Serve come "call-to-action" per incoraggiare l'utente a collegare
 * una prenotazione in spiaggia per lo stesso periodo, migliorando il flusso di lavoro.
 *
 * DESIGN CHOICES:
 * - **Material3 AlertDialog**: Utilizza il componente standard per garantire coerenza visiva
 *   con il resto dell'applicazione.
 * - **Statelessness**: Il componente non gestisce alcuno stato interno; riceve i dati (callback)
 *   direttamente dal chiamante, rendendolo facilmente testabile e riutilizzabile.
 *
 * @param onConfirm Funzione da eseguire quando l'utente accetta di andare alla spiaggia.
 * @param onDismiss Funzione da eseguire quando l'utente decide di non procedere o chiude il dialogo.
 */
@Composable
fun BeachPriorityDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Prenotazione Salvata")
        },
        text = {
            Text(text = "Il bungalow è stato prenotato con successo. Vuoi procedere ora a scegliere un ombrellone in spiaggia per lo stesso periodo?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sì, vai alla spiaggia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No, chiudi")
            }
        }
    )
}