@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.esame.ui.bookings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esame.data.model.Booking
import com.example.esame.data.model.Umbrella
import java.util.*

/**
 * Componente principale per la visualizzazione del tabellone ombrelloni.
 *
 * SCOPO:
 * Mostrare una griglia interattiva degli ombrelloni filtrabile per fila o disponibilità.
 * Gestisce lo stato visivo dei filtri e delega le azioni (click, long press) al chiamante.
 *
 * DESIGN CHOICES:
 * - **State Selection**: I filtri [showOnlyFree] e [selectedRow] sono mantenuti localmente
 *   per garantire una reattività immediata dell'interfaccia senza appesantire il ViewModel.
 * - **Efficiency**: Utilizza [remember] con chiavi multiple per ricalcolare la lista
 *   degli ombrelloni filtrati solo quando necessario.
 */

// CONFIGURAZIONE COLORI
private val PrimaryAccent = Color(0xFF1976D2)
private val GridLineColor = Color(0xFFEEEEEE)
private val BookingGreen = Color(0xFFE8F5E9)
private val BookingGreenText = Color(0xFF2E7D32)
private val BookingYellow = Color(0xFFFFFDE7)
private val BookingYellowText = Color(0xFFF57F17)
private val DisabledCell = Color(0xFFF9FAFB)

// FUNZIONI HELPER BASE
/**
 * Determina se un giorno specifico è prenotabile in base alla stagionalità.
 * Logica: Maggio (dal 15), Giugno-Agosto (tutto), Settembre (fino al 15).
 */
private fun isDateBookable(day: Int, calendar: Calendar): Boolean {
    val month = calendar.get(Calendar.MONTH)
    return when (month) {
        Calendar.MAY -> day >= 15
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> true
        Calendar.SEPTEMBER -> day <= 15
        else -> false
    }
}

/**
 * Normalizza una data impostando l'orario a mezzanotte in formato UTC.
 * Scelta: Fondamentale per il confronto corretto delle date nelle celle della griglia.
 */
private fun Date.startOfDay(): Date {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}


/**
 * COMPONENTE PRINCIPALE: BookingsScreen
 *
 * SCOPO:
 * Fornire la visualizzazione interattiva del tabellone ombrelloni (Booking Grid).
 *
 * DESIGN CHOICES:
 * - **State Hoisting**: Riceve lo stato dal ViewModel e solleva gli eventi tramite callback.
 * - **Local UI State**: Gestisce i filtri (showOnlyFree, selectedRow) internamente per massimizzare la reattività.
 * - **Lazy Loading**: Utilizza LazyColumn per gestire in modo efficiente liste lunghe di ombrelloni.
 */
@Composable
fun BookingsScreen(
    onBackClick: () -> Unit,
    umbrellas: List<Umbrella>,
    bookings: List<Booking>,
    isLoading: Boolean,
    currentDate: Calendar,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    onCellClick: (umbrellaId: String, day: Int) -> Unit,
    onBookingLongPress: (Booking) -> Unit,
    pendingBookings: List<Booking>,
    onShowPendingListClick: () -> Unit
) {
    // STATO DEI FILTRI
    var showOnlyFree by remember { mutableStateOf(false) }
    var selectedRow by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val filteredUmbrellas = remember(umbrellas, bookings, currentDate, showOnlyFree, selectedRow) {
                umbrellas.filter { umbrella ->
                    val passRow = if (selectedRow != null) {
                        umbrella.rowIndex == selectedRow?.toLong()
                    } else true

                    val passFree = if (showOnlyFree) {
                        !hasAnyBookingInVisibleRange(bookings, umbrella.id, currentDate)
                    } else true

                    passRow && passFree
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // BARRA DEI FILTRI VELOCI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ICONA LISTA D'ATTESA CON OROLOGIO E BADGE
                    BadgedBox(
                        badge = {
                            if (pendingBookings.isNotEmpty()) {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(pendingBookings.size.toString())
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onShowPendingListClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule, // Icona Orologio
                            contentDescription = "Lista d'Attesa",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Separatore visivo tra l'orologio e i filtri
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = GridLineColor
                    )

                    // Chips: File da 1 a 4
                    (1..4).forEach { rowNum ->
                        FilterChip(
                            selected = selectedRow == rowNum,
                            onClick = {
                                selectedRow = if (selectedRow == rowNum) null else rowNum
                            },
                            label = { Text("Fila $rowNum") },
                            leadingIcon = {
                                if (selectedRow == rowNum) Icon(Icons.Default.Check, null)
                            }
                        )
                    }

                    if (showOnlyFree || selectedRow != null) {
                        IconButton(onClick = { showOnlyFree = false; selectedRow = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Reset", tint = Color.Gray)
                        }
                    }
                }

                BookingGrid(
                    umbrellas = filteredUmbrellas,
                    bookings = bookings,
                    currentDate = currentDate,
                    onCellClick = onCellClick,
                    onBookingLongPress = onBookingLongPress,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}



/**
 * LOGICA DI FILTRAGGIO DISPONIBILITÀ
 */
private fun hasAnyBookingInVisibleRange(bookings: List<Booking>, umbrellaId: String?, currentDate: Calendar): Boolean {
    if (umbrellaId == null) return false
    val startDay = when (currentDate.get(Calendar.MONTH)) {
        Calendar.MAY, Calendar.SEPTEMBER -> 15
        else -> 1
    }
    val endDay = when (currentDate.get(Calendar.MONTH)) {
        Calendar.SEPTEMBER -> 15
        else -> currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val viewStartDate = (currentDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, startDay) }.time.startOfDay()
    val viewEndDate = (currentDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, endDay) }.time.startOfDay()

    return bookings.any { b ->
        if (b.umbrellaId != umbrellaId) return@any false
        val rawStart = b.startDate?.toDate()?.startOfDay() ?: return@any false
        val rawEnd = b.endDate?.toDate()?.startOfDay() ?: return@any false
        val checkStart: Date
        val checkEnd: Date

        if (b.seasonal) {
            val currentYear = currentDate.get(Calendar.YEAR)
            val calStart = Calendar.getInstance().apply { time = rawStart; set(Calendar.YEAR, currentYear) }
            val calEnd = Calendar.getInstance().apply { time = rawEnd; set(Calendar.YEAR, currentYear) }
            if (calEnd.before(calStart)) { calEnd.set(Calendar.YEAR, currentYear + 1) }
            checkStart = calStart.time
            checkEnd = calEnd.time
        } else {
            checkStart = rawStart
            checkEnd = rawEnd
        }
        !checkStart.after(viewEndDate) && !checkEnd.before(viewStartDate)
    }
}


/**
 * COMPONENTE: BookingGrid
 * Rappresenta la tabella bidimensionale degli ombrelloni.
 */
@Composable
fun BookingGrid(
    umbrellas: List<Umbrella>,
    bookings: List<Booking>,
    currentDate: Calendar,
    onCellClick: (umbrellaId: String, day: Int) -> Unit,
    onBookingLongPress: (Booking) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayHeaders = when (currentDate.get(Calendar.MONTH)) {
        Calendar.MAY -> (15..31).toList()
        Calendar.SEPTEMBER -> (1..15).toList()
        in Calendar.JUNE..Calendar.AUGUST -> (1..currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)).toList()
        else -> emptyList()
    }

    if (dayHeaders.isEmpty() || umbrellas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessuna disponibilità o ombrellone trovato", color = Color.Gray)
        }
        return
    }

    val groupedUmbrellas = umbrellas.groupBy { it.rowIndex }.toSortedMap()
    val horizontalScrollState = rememberScrollState()
    val headerHeight = 50.dp
    val rowHeight = 60.dp
    val cellWidth = 60.dp
    val umbrellaHeaderWidth = 70.dp

    LazyColumn(modifier = modifier) {
        // STICKY HEADER GIORNI
        stickyHeader {
            Row(Modifier.fillMaxWidth().background(Color.White).height(headerHeight)) {
                Box(Modifier.size(umbrellaHeaderWidth, headerHeight).background(Color.White).border(0.5.dp, GridLineColor))
                Row(Modifier.horizontalScroll(horizontalScrollState)) {
                    dayHeaders.forEach { day ->
                        DayHeaderCellModern(day.toString(), cellWidth, headerHeight)
                    }
                }
            }
            HorizontalDivider(color = GridLineColor)
        }

        // CORPO GRIGLIA
        groupedUmbrellas.forEach { (rowIndex, umbrellasInRow) ->
            item {
                Row(Modifier.fillMaxWidth().background(Color(0xFF1976D2)).padding(vertical = 6.dp, horizontal = 16.dp)) {
                    Text("FILA ${rowIndex ?: "?"}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }
                HorizontalDivider(color = GridLineColor)
            }

            items(umbrellasInRow, key = { it.id ?: UUID.randomUUID().toString() }) { umbrella ->
                Row(Modifier.fillMaxWidth()) {
                    UmbrellaHeaderCellModern(umbrella.number.toString(), umbrellaHeaderWidth, rowHeight)
                    Row(Modifier.horizontalScroll(horizontalScrollState)) {
                        dayHeaders.forEach { day ->
                            val bookingForCell = findBookingForDate(bookings, umbrella.id, day, currentDate)
                            val isCellEnabled = isDateBookable(day, currentDate)
                            BookingCellModern(
                                booking = bookingForCell,
                                isEnabled = isCellEnabled,
                                width = cellWidth,
                                height = rowHeight,
                                onClick = { if (bookingForCell == null && isCellEnabled) umbrella.id?.let { onCellClick(it, day) } },
                                onLongClick = { bookingForCell?.let { onBookingLongPress(it) } }
                            )
                        }
                    }
                }
                HorizontalDivider(color = GridLineColor, thickness = 0.5.dp)
            }
        }
    }
}



/**
 * FUNZIONI DI SUPPORTO PER IL RENDERING DELLE CELLE
 */
private fun findBookingForDate(bookings: List<Booking>, umbrellaId: String?, day: Int, calendar: Calendar): Booking? {
    if (umbrellaId == null) return null

    // 1. Definiamo la data della cella che stiamo disegnando
    val targetDate = (calendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, day)
    }.time.startOfDay()

    val targetMonth = calendar.get(Calendar.MONTH)

    return bookings.find { booking ->
        if (booking.umbrellaId != umbrellaId) return@find false

        // 2. LOGICA STAGIONALE:
        // Se la prenotazione è stagionale, deve coprire ogni cella visibile (Maggio-Settembre)
        if (booking.seasonal) {
            // Verifica se il mese della cella è nel range estivo (Maggio-Settembre)
            return@find targetMonth in Calendar.MAY..Calendar.SEPTEMBER
        }

        // 3. LOGICA NORMALE (NON STAGIONALE):
        val bookingStart = booking.startDate?.toDate()?.startOfDay() ?: return@find false
        val bookingEnd = booking.endDate?.toDate()?.startOfDay() ?: return@find false

        // La cella è occupata se la data target è compresa tra inizio e fine prenotazione
        targetDate.time in bookingStart.time..bookingEnd.time
    }
}
/**
 * Rappresenta la singola cella interattiva della griglia.
 *
 * DESIGN CHOICES:
 * Visual Feedback: Colori diversi per distinguere a colpo d'occhio il tipo di occupazione.
 * Combined Click: Supporta sia il click normale (nuova prenotazione) che quello lungo (opzioni).
 * Performance: Usa una [Surface] arrotondata interna per un look moderno senza appesantire il rendering.
 */
@Composable
fun BookingCellModern(
    booking: Booking?,
    isEnabled: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = when {
        !isEnabled -> DisabledCell
        booking == null -> Color.White
        booking.seasonal -> BookingYellow
        else -> BookingGreen
    }
    val textColor = when {
        !isEnabled -> Color.Gray
        booking == null -> Color.Transparent
        booking.seasonal -> BookingYellowText
        else -> BookingGreenText
    }
    val cellText = booking?.clientSurname?.take(3)?.uppercase() ?: ""

    Box(
        modifier = Modifier.size(width, height).background(Color.White).border(0.5.dp, GridLineColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isEnabled) Box(modifier = Modifier.fillMaxSize().background(DisabledCell))
        else if (booking != null) {
            Surface(color = backgroundColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Text(cellText, color = textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


/**
 * Cella dell'intestazione superiore che visualizza il numero del giorno.
 */
@Composable
fun DayHeaderCellModern(text: String, width: Dp, height: Dp) {
    Box(Modifier.size(width, height).border(0.5.dp, GridLineColor).background(Color.White), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}


/**
 * Cella laterale che identifica l'ombrellone con numero e icona.
 */
@Composable
fun UmbrellaHeaderCellModern(umbrellaNumber: String, width: Dp, height: Dp) {
    Row(
        modifier = Modifier.size(width, height).border(0.5.dp, GridLineColor).background(Color.White).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.BeachAccess, null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(umbrellaNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}