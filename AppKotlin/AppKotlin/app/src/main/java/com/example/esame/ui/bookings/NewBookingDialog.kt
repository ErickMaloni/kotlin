@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.esame.ui.bookingsimport

import androidx.compose.foundation.clickable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.esame.data.model.Booking
import java.text.SimpleDateFormat
import java.util.*


/**
 * NewBookingDialog rappresenta l'interfaccia di inserimento per una nuova prenotazione spiaggia.
 *
 * SCOPO:
 * Permettere all'utente di inserire i dati del cliente e selezionare il periodo di permanenza,
 * calcolando in tempo reale il preventivo e bloccando le date già occupate.
 *
 * DESIGN CHOICES:
 * - **ModalBottomSheet**: Utilizzato per fornire un'esperienza "mobile-first" fluida che non interrompe
 *   completamente il contesto visivo della griglia sottostante.
 * - **DerivedStateOf**: Utilizzato per il calcolo della validità del pulsante "Salva", ottimizzando
 *   le performance evitando ricalcoli inutili durante la ricomposizione.
 * - **SelectableDates**: Implementazione di una logica custom nel DatePicker per garantire l'integrità
 *   dei dati (impedisce sovrapposizioni di prenotazioni sullo stesso ombrellone).
 *
 * @param startDate Data selezionata inizialmente sulla griglia.
 * @param previewPrice Prezzo calcolato dinamicamente dal ViewModel.
 * @param bookingsForUmbrella Lista delle prenotazioni esistenti per l'ombrellone selezionato (per il filtraggio date).
 * @param onDateChange Callback invocata quando l'utente modifica la data di fine o il tipo di abbonamento.
 * @param onDismiss Callback per la chiusura del dialogo senza salvataggio.
 * @param onSave Callback per confermare e persistere la prenotazione.
 */
@Composable
fun NewBookingDialog(
    startDate: Date,
    previewPrice: Double,
    bookingsForUmbrella: List<Booking>,
    onDateChange: (endDate: Date, isSeasonal: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, surname: String, endDate: Date, isSeasonal: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var endDate by remember(startDate) { mutableStateOf(startDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) }
    var isSeasonal by remember { mutableStateOf(false) }

    // Gestione dello stato del BottomSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    /**
     * Logica di validazione del modulo.
     * Il salvataggio è abilitato solo se i campi anagrafici sono compilati e la data è coerente.
     */
    val isSaveEnabled by remember(name, surname, endDate, isSeasonal) {
        derivedStateOf {
            name.isNotBlank() && surname.isNotBlank() && (isSeasonal || !endDate.before(startDate))
        }
    }

    LaunchedEffect(endDate, isSeasonal) {
        onDateChange(endDate, isSeasonal)
    }

    // LOGICA DATE PICKER  CON BLOCCO DATE OCCUPATE
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.time,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dateToCheck = Date(utcTimeMillis)

                    // Limite: Non si può selezionare una data precedente alla data di inizio (click sulla griglia)
                    val startCal = Calendar.getInstance().apply {
                        time = startDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (dateToCheck.before(startCal.time)) return false

                    // LOGICA DI BLOCCO: Verifica se la data è occupata da altre prenotazioni per questo ombrellone
                    val isOccupied = bookingsForUmbrella.any { booking ->
                        val bStart = booking.startDate?.toDate()
                        val bEnd = booking.endDate?.toDate()

                        if (bStart != null && bEnd != null) {
                            // Creiamo calendari per ignorare ore/minuti/secondi nel confronto
                            val checkCal = Calendar.getInstance().apply { time = dateToCheck }
                            val existingStart = Calendar.getInstance().apply { time = bStart }
                            val existingEnd = Calendar.getInstance().apply { time = bEnd }

                            // La data è occupata se: (DataCalendario >= InizioPrenotazione) E (DataCalendario <= FinePrenotazione)
                            !checkCal.before(existingStart) && !checkCal.after(existingEnd)
                        } else false
                    }

                    return !isOccupied // Restituisce true (selezionabile) solo se NON è occupato
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = Date(millis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // INTERFACCIA MODAL BOTTOM SHEET
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Nuova Prenotazione",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Cognome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = if (isSeasonal) "Tutta la stagione" else dateFormatter.format(endDate),
                onValueChange = {},
                readOnly = true,
                enabled = !isSeasonal,
                label = { Text("Data Fine") },
                trailingIcon = {
                    IconButton(onClick = { if(!isSeasonal) showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Seleziona data")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSeasonal) { showDatePicker = true }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSeasonal = !isSeasonal }
            ) {
                Checkbox(checked = isSeasonal, onCheckedChange = { isSeasonal = it })
                Text("Prenotazione Stagionale")
            }

            if (previewPrice > 0) {
                Text(
                    text = "Prezzo Totale: ${"%.2f".format(previewPrice)} €",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSave(name, surname, endDate, isSeasonal) },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salva Prenotazione")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annulla", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}