@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.esame.ui.bungalowbookings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.esame.data.model.BungalowBooking
import com.example.esame.ui.bookings.BookingsViewModel // Assicurati che questo gestisca entrambi o usa BungalowBookingsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * BungalowBookingsRoute è il "punto di ingresso" logico per la gestione del tabellone Bungalow.
 *
 * SCOPO:
 * Orchestrare la comunicazione tra il [BookingsViewModel] e i vari componenti UI (Screen, Dialog, Sheets),
 * gestendo la navigazione temporale, la visualizzazione dei dialoghi e gli effetti collaterali (Side Effects).
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: La logica di business è completamente delegata al ViewModel. Questa Route osserva
 *   lo stato e reagisce ai cambiamenti, mantenendo i componenti UI "stupidi".
 * - **Side Effect Management**: [LaunchedEffect] viene utilizzato per gestire eventi una-tantum come
 *   la visualizzazione di Toast, garantendo che non vengano rieseguiti a ogni ricomposizione.
 * - **Modularità**: La UI è suddivisa in componenti più piccoli e specializzati (es. `BungalowBookingInfoSheet`)
 *   per migliorare la leggibilità e la riusabilità.
 */
@Composable
fun BungalowBookingsRoute(
    viewModel: BookingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToBeach: (startDate: Long, endDate: Long, bookingId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentDate by viewModel.currentDate
    val context = LocalContext.current

    /**
     * GESTIONE FEEDBACK UTENTE (TOAST)
     * Reagisce alla presenza di un messaggio nel `toastMessage` dello stato.
     * Una volta visualizzato, notifica il ViewModel per "consumare" l'evento e resettare il messaggio.
     */
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onToastMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ITALIAN) }
                    Text(
                        monthFormat.format(currentDate.time).replaceFirstChar { it.titlecase(Locale.ITALIAN) },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mese precedente", modifier = Modifier.rotate(180f))
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mese successivo")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            BungalowBookingsScreen(
                bungalows = uiState.bungalows,
                bookings = uiState.bungalowBookings,
                isLoading = uiState.isLoading,
                currentDate = currentDate,
                onCellClick = viewModel::onCellClick,
                onBookingLongPress = { booking ->
                    viewModel.onBookingOptionsClick(booking)
                }
            )
        }
    }

    // BOTTOM SHEET MENU OPZIONI
    if (uiState.showInfoSheet && uiState.selectedBooking != null) {
        val booking = uiState.selectedBooking!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissBookingOptionsDialog() },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Opzioni per ${booking.clientName} ${booking.clientSurname}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("Visualizza Info") },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.clickable { viewModel.onShowDetailsClick() }
                )
                ListItem(
                    headlineContent = { Text("Modifica Prenotazione") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable { viewModel.onEditBookingClick() }
                )
                ListItem(
                    headlineContent = { Text("Cancella Prenotazione", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.clickable { viewModel.onDeleteClick() }
                )
            }
        }
    }

    // DETTAGLI COMPLETI DELLA PRENOTAZIONE
    if (uiState.showDetailsSheet && uiState.selectedBooking != null) {
        BungalowBookingInfoSheet(
            booking = uiState.selectedBooking!!,
            beachBookings = uiState.bookingsBeach,
            umbrellas = uiState.umbrellasBeach,
            onDismiss = { viewModel.onDismissDetailsSheet() }
        )
    }

    // DIALOGO PER NUOVA PRENOTAZIONE O MODIFICA
    if (uiState.showBookingDialog) {
        val initialStartDate = if (uiState.isEditMode) {
            uiState.selectedBooking?.startDate?.toDate() ?: Date()
        } else {
            uiState.selectedCellData?.let { (_, dayOfMonth) ->
                (currentDate.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }.time
            } ?: Date()
        }

        NewBungalowBookingDialog(
            startDate = initialStartDate,
            previewPrice = uiState.previewPrice,

            // LOGICA DI BLOCCO DATE:
            // Passiamo la lista delle prenotazioni per questo bungalow
            bookingsForBungalow = remember(uiState.selectedCellData, uiState.bungalowBookings) {
                val bId = if (uiState.isEditMode) uiState.selectedBooking?.bungalowId else uiState.selectedCellData?.first
                uiState.bungalowBookings.filter { it.bungalowId == bId }
            },
            onDateChange = { updatedStartDate, endDate, numPeople, hasPets, seasonal ->
                val bId = if (uiState.isEditMode) uiState.selectedBooking?.bungalowId else uiState.selectedCellData?.first
                bId?.let { id ->
                    viewModel.calculateBungalowPricePreview(
                        start = updatedStartDate,
                        endDate = endDate,
                        bungalowId = id,
                        numPeople = numPeople,
                        hasPets = hasPets,
                        isSeasonal = seasonal
                    )
                }
            },
            onDismiss = viewModel::onDismissDialog,
            onSave = { name, surname, endDate, numPeople, hasPets, seasonal ->
                // Normalizziamo la data di fine a mezzogiorno per evitare sfasamenti di fuso orario
                val normalizedEndDate = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                viewModel.onSaveBungalowBooking(
                    name = name,
                    surname = surname,
                    endDate = endDate,
                    numPeople = numPeople,
                    hasPets = hasPets,
                    isSeasonal = seasonal
                )
            }
        )
    }

    // DIALOGO DI CONFERMA CANCELLAZIONE
    if (uiState.showDeleteConfirmation) {AlertDialog(
        onDismissRequest = { viewModel.onDismissDeleteDialog() },
        shape = RoundedCornerShape(28.dp),
        title = { Text("Conferma Cancellazione", fontWeight = FontWeight.SemiBold) },
        text = { Text("Sei sicuro di voler cancellare la prenotazione per ${uiState.selectedBooking?.clientName} ${uiState.selectedBooking?.clientSurname}?") },
        confirmButton = {
            Button(
                onClick = { viewModel.onConfirmDelete() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Cancella", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = { viewModel.onDismissDeleteDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF495D92)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Annulla", color = Color.White)
            }
        }
    )
    }

    // DIALOGO POST-SALVATAGGIO PER PRIORITÀ SPIAGGIA
    if (uiState.showBeachPriorityDialog) {
        BeachPriorityDialog(
            onConfirm = {
                uiState.lastSavedBooking?.let { booking ->
                    val start = booking.startDate?.toDate()?.time ?: 0L
                    val end = booking.endDate?.toDate()?.time ?: 0L
                    val bId = booking.bookingId ?: ""
                    onNavigateToBeach(start, end, bId)
                }
                viewModel.onDismissBeachPriority()
            },
            onDismiss = viewModel::onDismissBeachPriority
        )
    }
}

// COMPONENTI UI DI SUPPORTO

/**
 * Menu delle opzioni rapide per una prenotazione Bungalow (Visualizza, Modifica, Cancella).
 */
@Composable
fun BungalowBookingInfoSheet(
    booking: BungalowBooking,
    beachBookings: List<com.example.esame.data.model.Booking>,
    umbrellas: List<com.example.esame.data.model.Umbrella>,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN) }

    // Logica per trovare NUMERO e FILA dell'ombrellone associato
    val linkedUmbrellaInfo = remember(booking.linkedUmbrellaBookingId, beachBookings, umbrellas) {
        val beachBooking = beachBookings.find { it.id == booking.linkedUmbrellaBookingId }
        val umbrella = umbrellas.find { it.id == beachBooking?.umbrellaId }

        if (umbrella != null) {
            "Ombrellone n° ${umbrella.number} - Fila ${umbrella.rowIndex}"
        } else {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(
                    "Dettagli Prenotazione",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${booking.clientName} ${booking.clientSurname}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

            InfoRow(
                Icons.Default.DateRange,
                "Periodo",
                "${dateFormatter.format(booking.startDate?.toDate() ?: Date())} - ${dateFormatter.format(booking.endDate?.toDate() ?: Date())}"
            )

            // MOSTRA INFO OMBRELLONE COLLEGATO
            if (linkedUmbrellaInfo != null) {
                InfoRow(
                    icon = Icons.Default.BeachAccess,
                    label = "Ombrellone prenotato",
                    value = linkedUmbrellaInfo,
                    valueColor = Color(0xFF2E7D32)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    InfoRow(Icons.Default.Groups, "Ospiti", "${booking.numPeople} Persone")
                }
                Box(modifier = Modifier.weight(1f)) {
                    InfoRow(Icons.Default.Pets, "Animali", if (booking.hasPets) "Presenti" else "Assenti")
                }
            }

            InfoRow(
                Icons.Default.Payments,
                "Totale Pagato",
                "${"%.2f".format(booking.totalPrice)} €",
                MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF495D92)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Chiudi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Dialogo che chiede all'utente se desidera prenotare anche un ombrellone
 * dopo aver salvato con successo una prenotazione bungalow.
 */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}