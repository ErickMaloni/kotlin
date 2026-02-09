@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.esame.ui.bungalowbookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.esame.data.model.BungalowBooking
import java.text.SimpleDateFormat
import java.util.*

/**
 * NewBungalowBookingDialog gestisce l'interfaccia per la creazione di una nuova prenotazione Bungalow.
 *
 * SCOPO:
 * Fornire un modulo interattivo per inserire dati anagrafici e parametri di soggiorno (persone, animali,
 * stagionalità), applicando vincoli temporali specifici (es. check-out obbligatorio di Sabato).
 *
 * DESIGN CHOICES:
 * - **ModalBottomSheet**: Utilizzato per fornire un'esperienza utente moderna e focalizzata sul modulo.
 * - **DerivedStateOf**: Ottimizza il calcolo della validità del pulsante "Salva" ricalcolandolo solo
 *   quando cambiano le dipendenze essenziali.
 * - **Saturday Logic**: Implementa una logica di default che calcola la fine del soggiorno
 *   automaticamente al Sabato successivo (minimo 7 giorni).
 * - **LaunchedEffect**: Sincronizza lo stato locale con il ViewModel per il calcolo dinamico del prezzo.
 *
 * @param startDate Data di inizio selezionata (Check-in).
 * @param previewPrice Preventivo calcolato in tempo reale dal ViewModel.
 * @param bookingsForBungalow Lista delle prenotazioni esistenti per il controllo conflitti.
 * @param onDateChange Notifica il ViewModel per ricalcolare il prezzo quando cambiano i parametri.
 * @param onDismiss Chiude il dialogo senza salvare.
 * @param onSave Persiste la prenotazione nel database.
 */
@Composable
fun NewBungalowBookingDialog(
    startDate: Date,
    previewPrice: Double,
    bookingsForBungalow: List<BungalowBooking>,
    onDateChange: (startDate: Date, endDate: Date, numPeople: Int, hasPets: Boolean, isSeasonal: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, surname: String, endDate: Date, numPeople: Int, hasPets: Boolean, isSeasonal: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var isSeasonal by remember { mutableStateOf(false) }

    /**
     * LOGICA CALCOLO DATA FINE PREDEFINITA (Check-out di Sabato)
     * Calcola la fine del soggiorno aggiungendo 7 giorni e cercando il primo sabato disponibile.
     */
    val initialEndDate = remember(startDate) {
        Calendar.getInstance().apply {
            time = startDate
            add(Calendar.DAY_OF_YEAR, 7)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.time
    }

    var endDate by remember(startDate) { mutableStateOf(initialEndDate) }
    var numPeople by remember { mutableIntStateOf(2) }
    var hasPets by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Reset stagionale se cambiano le persone
    LaunchedEffect(numPeople) {
        if (numPeople != 2) isSeasonal = false
    }

    // Se è stagionale, la data di fine è fissa
    val finalEndDate = remember(endDate, isSeasonal, startDate) {
        if (isSeasonal) {
            Calendar.getInstance().apply {
                time = startDate // Prende l'anno dalla data di inizio
                set(Calendar.MONTH, Calendar.SEPTEMBER)
                set(Calendar.DAY_OF_MONTH, 15)
                set(Calendar.HOUR_OF_DAY, 12) // Normalizza l'orario
            }.time
        } else endDate
    }


    /**
     * VALIDAZIONE DEL MODULO
     * Abilita il salvataggio solo se i dati obbligatori sono presenti.
     */
    val isSaveEnabled by remember(name, surname, finalEndDate) {
        derivedStateOf { name.isNotBlank() && surname.isNotBlank() }
    }

    // NOTIFICA IL VIEWMODEL:
    LaunchedEffect(startDate, finalEndDate, numPeople, hasPets, isSeasonal) {
        onDateChange(startDate, finalEndDate, numPeople, hasPets, isSeasonal)
    }

    // LOGICA DATE PICKER
    if (showDatePicker && !isSeasonal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.time,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dateToCheck = Date(utcTimeMillis)
                    val cal = Calendar.getInstance().apply { timeInMillis = utcTimeMillis }
                    val isSaturday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    val isAfterStart = utcTimeMillis > startDate.time

                    val isOccupied = bookingsForBungalow.any { booking ->
                        val bStart = booking.startDate?.toDate()
                        val bEnd = booking.endDate?.toDate()
                        if (bStart != null && bEnd != null) {
                            !dateToCheck.before(bStart) && !dateToCheck.after(bEnd)
                        } else false
                    }
                    return isSaturday && isAfterStart && !isOccupied
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = Date(it) }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nuova Prenotazione Bungalow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nome Cliente") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            OutlinedTextField(
                value = surname, onValueChange = { surname = it },
                label = { Text("Cognome Cliente") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Persone:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (numPeople > 2) numPeople-- }, enabled = !isSeasonal) {
                    Icon(Icons.Default.Remove, null)
                }
                Text("$numPeople", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                IconButton(onClick = { numPeople++ }, enabled = !isSeasonal) {
                    Icon(Icons.Default.Add, null)
                }
            }

            // In NewBungalowBookingDialog.kt (attorno alla riga 180)

            if (numPeople == 2) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Prenotazione Stagionale (15/05-15/09):", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isSeasonal,
                        onCheckedChange = { checked ->
                            // 1. Aggiorna lo stato locale dello switch
                            isSeasonal = checked

                            // 2. FORZA L'AGGIORNAMENTO DEL PREZZO
                            // Notifica immediatamente il ViewModel del cambio di stato stagionale.
                            onDateChange(startDate, finalEndDate, numPeople, hasPets, checked)
                        }
                    )
                }
            }


            OutlinedTextField(
                value = if (isSeasonal) "15/09/2026" else dateFormatter.format(endDate),
                onValueChange = {},
                readOnly = true,
                enabled = !isSeasonal,
                label = { Text(if (isSeasonal) "Periodo Fisso" else "Data Fine (Sabato)") },
                trailingIcon = {
                    if (!isSeasonal) {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isSeasonal) { showDatePicker = true }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Presenza Animali (+20€):", modifier = Modifier.weight(1f), color = if (isSeasonal) Color.Gray else Color.Unspecified)
                Switch(checked = if (isSeasonal) false else hasPets, onCheckedChange = { hasPets = it }, enabled = !isSeasonal)
            }

            HorizontalDivider(thickness = 0.5.dp)

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Totale: ${"%.2f".format(previewPrice)} €",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { onSave(name, surname, finalEndDate, numPeople, hasPets, isSeasonal) },
                enabled = isSaveEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salva Prenotazione")
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annulla", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}