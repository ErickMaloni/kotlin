@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.esame.ui.bookings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.esame.data.model.Booking
import com.example.esame.data.model.Umbrella
import com.example.esame.ui.bookingsimport.NewBookingDialog
import java.text.SimpleDateFormat
import java.util.*



/**
 * BookingsRoute funge da "Entry Point" logico per la gestione del tabellone prenotazioni spiaggia.
 *
 * SCOPO:
 * Gestire la comunicazione tra il [BookingsViewModel] e i vari componenti UI (Screen, Dialog, BottomSheets).
 * Gestisce la navigazione, la sincronizzazione delle date e gli effetti collaterali (Side Effects).
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: La logica di business è delegata interamente al ViewModel.
 * - **Side Effect Management**: Utilizzo di [LaunchedEffect] per gestire eventi asincroni o una-tantum (Toast e navigazione).
 * - **Modularità**: Suddivisione della UI in componenti piccoli e riutilizzabili (InfoRow, PendingItem).
 * - **OptIn a livello di file**: Scelta per evitare di sporcare ogni singola funzione con annotazioni sperimentali per Material 3.
 */
@Composable
fun BookingsRoute(
    onBackClick: () -> Unit,
    initialDateMillis: Long = -1L,
    bungalowBookingId: String = "",
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentDate by viewModel.currentDate
    val context = LocalContext.current

    /**
     * GESTIONE FEEDBACK UTENTE (TOAST)
     * Reagisce alla presenza di un messaggio nel toastMessage dello stato.
     * Una volta visualizzato, notifica il ViewModel per resettare il messaggio (evento "consumato").
     */
    // LOGICA PER MOSTRARE GLI ERRORI (L'ALERT)
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onToastMessageShown()
        }
    }

    // SINCRONIZZAZIONE INIZIALE
    /**
     * Inizializza la data del tabellone se passata tramite navigazione (es: da bungalow).
     * Imposta il legame con una prenotazione bungalow se presente.
     */
    LaunchedEffect(initialDateMillis, bungalowBookingId) {
        if (initialDateMillis != -1L) {
            viewModel.initDate(initialDateMillis)
        }
        if (bungalowBookingId.isNotEmpty()) {
            viewModel.setSourceBungalow(bungalowBookingId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ITALIAN) }
                        Text(monthFormat.format(currentDate.time).replaceFirstChar { it.titlecase(Locale.ITALIAN) })

                        // Indicatore visivo per l'utente in fase di spostamento prenotazione
                        if (uiState.bookingToRebook != null) {
                            Text(
                                "MODALITÀ SPOSTAMENTO ATTIVA",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (uiState.bookingToRebook != null) {
                        TextButton(onClick = { viewModel.cancelRebooking() }) {
                            Text("Annulla", color = Color.Red)
                        }
                    }
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "Mese precedente")
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "Mese successivo")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BookingsScreen(
                onBackClick = onBackClick,
                umbrellas = uiState.umbrellas,
                bookings = uiState.bookings,
                pendingBookings = uiState.pendingBookings,
                isLoading = uiState.isLoading,
                currentDate = currentDate,
                onNextMonth = viewModel::nextMonth,
                onPreviousMonth = viewModel::previousMonth,
                onCellClick = viewModel::onCellClick,
                onBookingLongPress = viewModel::onBookingOptionsClick,
                onShowPendingListClick = viewModel::onShowPendingBookingsClick
            )
        }
    }

    // LOGICA OVERLAY E DIALOGHI
    /**
     * DIALOG NUOVA PRENOTAZIONE
     * Viene mostrato quando l'utente seleziona una cella libera.
     * Filtra le prenotazioni per l'ombrellone selezionato per aiutare il controllo disponibilità lato UI.
     */
    if (uiState.showBookingDialog) {
        // Recuperiamo l'ID dell'ombrellone selezionato
        val selectedUmbrellaId = uiState.selectedCellData?.first

        // FILTRIAMO le prenotazioni esistenti per questo specifico ombrellone
        val bookingsForThisUmbrella = remember(selectedUmbrellaId, uiState.bookings) {
            if (selectedUmbrellaId != null) {
                uiState.bookings.filter { it.umbrellaId == selectedUmbrellaId }
            } else {
                emptyList()
            }
        }

        NewBookingDialog(
            startDate = remember(uiState.selectedCellData) {
                val cal = currentDate.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, uiState.selectedCellData?.second ?: 1)
                cal.time
            },
            previewPrice = uiState.previewPrice,
            // PASSIAMO LA LISTA FILTRATA AL DIALOG
            bookingsForUmbrella = bookingsForThisUmbrella,
            onDateChange = { endDate, isSeasonal ->
                viewModel.calculatePricePreview(endDate, isSeasonal)
            },
            onDismiss = { viewModel.onDismissDialog() },
            onSave = { name, surname, endDate, isSeasonal ->
                // Aggiungiamo 'context' (che è già definito alla riga 54)
                viewModel.onSaveBooking(
                    context = context,
                    clientName = name,
                    clientSurname = surname,
                    endDate = endDate,
                    isSeasonal = isSeasonal
                )
            }
        )
    }

    /**
     * MENU OPZIONI (BOTTOM SHEET)
     * Offre azioni rapide su una prenotazione esistente (Info, Modifica, Attesa, Cancella).
     */    if (uiState.showBookingOptionsDialog && uiState.selectedBookingForOptions != null) {
        BookingOptionsBottomSheet(
            booking = uiState.selectedBookingForOptions!!,
            onDismiss = { viewModel.onDismissBookingOptionsDialog() },
            onDeleteClick = { viewModel.onDeleteClick() },
            onEditClick = { viewModel.onEditClick() },
            onInfoClick = { viewModel.onInfoClick() },
            onPlaceOnHoldClick = { viewModel.placeBookingOnHold() }
        )
    }

    // DETTAGLI INFO
    if (uiState.showInfoDialog && uiState.selectedBookingForOptions != null) {
        BookingInfoSheet(
            booking = uiState.selectedBookingForOptions!!,
            umbrellas = uiState.umbrellas,
            onDismiss = { viewModel.onDismissInfoDialog() }
        )
    }

    //  CONFERMA CANCELLAZIONE
    if (uiState.showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDeleteDialog() },
            shape = RoundedCornerShape(28.dp), // Angoli arrotondati per il Dialog
            title = {
                Text(
                    "Conferma Cancellazione",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Sei sicuro di voler eliminare la prenotazione per ${uiState.selectedBookingForOptions?.clientSurname}?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteBooking() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)), // Rosso scuro
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Cancella", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.onDismissDeleteDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), // Blu grigio
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Annulla", color = Color.White)
                }
            }
        )
    }

    /**
     * LISTA D'ATTESA (PENDING BOOKINGS)
     * Visualizza le prenotazioni in attesa di riallocazione.
     */
    if (uiState.showPendingBookingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissPendingBookingsSheet() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)
            ) {
                Text("Prenotazioni in attesa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                if (uiState.pendingBookings.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Nessuna prenotazione in attesa", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.pendingBookings) { pending ->
                            PendingBookingItem(
                                booking = pending,
                                onRebookClick = {
                                    viewModel.startRebookingProcess(pending)
                                    viewModel.onDismissPendingBookingsSheet()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// COMPONENTI UI

/**
 * Rappresenta un singolo elemento nella lista delle prenotazioni in attesa.
 */
@Composable
fun PendingBookingItem(booking: Booking, onRebookClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${booking.clientName} ${booking.clientSurname}".uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Dal ${sdf.format(booking.startDate?.toDate() ?: Date())} al ${sdf.format(booking.endDate?.toDate() ?: Date())}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Button(onClick = onRebookClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), shape = RoundedCornerShape(8.dp)) {
                Text("Rialloca", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Foglio informativo che mostra i dettagli della prenotazione spiaggia.
 */
@Composable
fun BookingInfoSheet(booking: Booking, umbrellas: List<Umbrella>, onDismiss: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN) }
    val umbrellaNumber = umbrellas.find { it.id == booking.umbrellaId }?.number?.toString() ?: "N/D"

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text("Dettagli Prenotazione Ombrellone n° $umbrellaNumber", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${booking.clientName} ${booking.clientSurname}".uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            InfoRowSpiaggia(Icons.Default.DateRange, "Periodo", "${sdf.format(booking.startDate?.toDate() ?: Date())} - ${sdf.format(booking.endDate?.toDate() ?: Date())}")
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { InfoRowSpiaggia(Icons.Default.BeachAccess, "Tipo", if (booking.seasonal) "Stagionale" else "Giornaliero") }
                Box(modifier = Modifier.weight(1f)) { InfoRowSpiaggia(Icons.Default.Payments, "Prezzo", "${"%.2f".format(booking.totalPrice)} €", Color(0xFF2E7D32)) }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Chiudi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

/**
 * Componente per visualizzare una riga di informazione (Icona + Label + Valore).
 */
@Composable
fun InfoRowSpiaggia(icon: ImageVector, label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
}


/**
 * Menu delle opzioni per una prenotazione (Info, Modifica, Attesa, Elimina).
 */
@Composable
fun BookingOptionsBottomSheet(
    booking: Booking,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onInfoClick: () -> Unit,
    onPlaceOnHoldClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Opzioni per ${booking.clientSurname}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ListItem(
                headlineContent = { Text("Visualizza Info") },
                leadingContent = { Icon(Icons.Default.Info, null) },
                modifier = Modifier.clickable { onInfoClick() }
            )
            ListItem(
                headlineContent = { Text("Modifica Prenotazione") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable { onEditClick() }
            )
            ListItem(
                headlineContent = { Text("Metti in attesa") },
                leadingContent = { Icon(Icons.Default.PauseCircle, null) },
                modifier = Modifier.clickable { onPlaceOnHoldClick() }
            )
            ListItem(
                headlineContent = { Text("Cancella Prenotazione", color = Color.Red) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                modifier = Modifier.clickable { onDeleteClick() }
            )
        }
    }
}