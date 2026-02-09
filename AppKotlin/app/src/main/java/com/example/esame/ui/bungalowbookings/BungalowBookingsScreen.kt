package com.example.esame.ui.bungalowbookings

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
import com.example.esame.data.model.Bungalow
import com.example.esame.data.model.BungalowBooking
import java.util.*

// CONFIGURAZIONE COLORI
private val PrimaryAccent = Color(0xFF1976D2)
private val GoldAccent = Color(0xFFB8860B) // Oro scuro per il Deluxe (più leggibile)
private val GoldBackground = Color(0xFFFFF9E1) // Sfondo giallino tenue per la cella Deluxe
private val GridLineColor = Color(0xFFEEEEEE)
private val BookingGreen = Color(0xFFE8F5E9)
private val BookingGreenText = Color(0xFF2E7D32)
private val BookingYellow = Color(0xFFFFFDE7)
private val BookingYellowText = Color(0xFFF57F17)
private val BookingBlue = Color(0xFFE3F2FD)
private val BookingBlueText = Color(0xFF1976D2)
private val SectionBackground = Color(0xFFF5F7F9)

@Composable
fun BungalowBookingsScreen(
    bungalows: List<Bungalow>,
    bookings: List<BungalowBooking>,
    isLoading: Boolean,
    currentDate: Calendar,
    onCellClick: (bungalowId: String, day: Int) -> Unit,
    onBookingLongPress: (BungalowBooking) -> Unit
) {
    var showOnlyDeluxe by remember { mutableStateOf(false) }
    var selectedCapacity by remember { mutableStateOf<Long?>(null) }

    // LOGICA DI FILTRAGGIO
    val filteredBungalows = remember(bungalows, selectedCapacity, showOnlyDeluxe) {
        bungalows.filter { bungalow ->
            val passCapacity = if (selectedCapacity != null) bungalow.capacity == selectedCapacity else true
            val passDeluxe = if (showOnlyDeluxe) bungalow.type == "DELUXE" else true
            passCapacity && passDeluxe
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            // TUTTO IL CONTENUTO DEVE STARE DENTRO L'ELSE DI ISLOADING
            Column(modifier = Modifier.fillMaxSize()) {

                // BARRA FILTRI VELOCI
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // IL FILTRO DELUXE VA FUORI DAL LOOP DEI POSTI LETTO
                    FilterChip(
                        selected = showOnlyDeluxe,
                        onClick = { showOnlyDeluxe = !showOnlyDeluxe },
                        label = { Text("Solo Deluxe") },
                        leadingIcon = if (showOnlyDeluxe) {
                            { Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldBackground,
                            selectedLabelColor = GoldAccent
                        )
                    )

                    listOf(2L, 3L, 4L, 5L).forEach { cap ->
                        FilterChip(
                            selected = selectedCapacity == cap,
                            onClick = { selectedCapacity = if (selectedCapacity == cap) null else cap },
                            label = { Text("$cap Posti") }
                        )
                    }
                }

                // GRIGLIA
                BungalowGridModern(
                    bungalows = filteredBungalows,
                    bookings = bookings,
                    currentDate = currentDate,
                    onCellClick = onCellClick,
                    onBookingLongPress = onBookingLongPress
                )
            }
        }
    }
}

/**
 * Implementazione tecnica della griglia bidimensionale.
 *
 * DESIGN CHOICES:
 * - **Grouping**: Raggruppa i bungalow per tipologia per inserire i separatori di sezione.
 * - **Sticky Headers**: Mantiene visibile la riga delle date durante lo scorrimento verticale.
 * - **Shared Scroll State**: Utilizza un unico [horizontalScrollState] per sincronizzare
 *   lo scorrimento tra l'intestazione dei giorni e le celle dei bungalow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BungalowGridModern(
    bungalows: List<Bungalow>,
    bookings: List<BungalowBooking>,
    currentDate: Calendar,
    onCellClick: (bungalowId: String, day: Int) -> Unit,
    onBookingLongPress: (BungalowBooking) -> Unit
) {
    val dayHeaders = remember(currentDate) {
        val month = currentDate.get(Calendar.MONTH)
        when (month) {
            Calendar.MAY -> (15..31).toList()
            Calendar.SEPTEMBER -> (1..15).toList()
            in Calendar.JUNE..Calendar.AUGUST -> (1..currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)).toList()
            else -> emptyList()
        }
    }

    // Raggruppamento speciale per separare il Deluxe
    val groupedBungalows = remember(bungalows) {
        bungalows.groupBy { bungalow ->
            if (bungalow.type == "DELUXE") "DELUXE" else "${bungalow.capacity} POSTI LETTO"
        }.toSortedMap { a, b ->
            // Ordina mettendo DELUXE per primo, poi per capacità
            if (a == "DELUXE") -1 else if (b == "DELUXE") 1 else a.compareTo(b)
        }
    }

    val horizontalScrollState = rememberScrollState()
    val sidebarWidth = 80.dp
    val cellWidth = 60.dp
    val rowHeight = 60.dp

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            stickyHeader {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .height(50.dp)
                ) {
                    Box(
                        Modifier
                            .size(sidebarWidth, 50.dp)
                            .border(0.5.dp, GridLineColor)
                            .background(Color.White)
                    )
                    Row(Modifier.horizontalScroll(horizontalScrollState)) {
                        dayHeaders.forEach { day ->
                            DayHeaderCellModern(day.toString(), cellWidth, 50.dp)
                        }
                    }
                }
            }

            groupedBungalows.forEach { (category, bungalowsInRow) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SectionBackground)
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (category == "DELUXE") GoldAccent else PrimaryAccent,
                            letterSpacing = 1.sp
                        )
                    }
                    HorizontalDivider(color = GridLineColor)
                }

                items(bungalowsInRow) { bungalow ->
                    Row(Modifier.fillMaxWidth()) {
                        // Implementazione della testata B1, B2... con stile Oro se Deluxe
                        BungalowHeaderCellModern(
                            number = bungalow.number.toString(),
                            width = sidebarWidth,
                            height = rowHeight,
                            isDeluxe = bungalow.type == "DELUXE"
                        )

                        Row(Modifier.horizontalScroll(horizontalScrollState)) {
                            dayHeaders.forEach { day ->
                                val booking = findBungalowBooking(bookings, bungalow.id, day, currentDate)
                                BungalowCellModern(
                                    booking = booking,
                                    width = cellWidth,
                                    height = rowHeight,
                                    onClick = { if (booking == null) onCellClick(bungalow.id, day) },
                                    onLongClick = { booking?.let { onBookingLongPress(it) } }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = GridLineColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Cella laterale che identifica il bungalow.
 * Cambia icona e colore se la tipologia è Deluxe.
 */
@Composable
fun BungalowHeaderCellModern(number: String, width: Dp, height: Dp, isDeluxe: Boolean = false) {
    Row(
        modifier = Modifier
            .size(width, height)
            .border(0.5.dp, GridLineColor)
            .background(if (isDeluxe) GoldBackground else Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isDeluxe) Icons.Default.Star else Icons.Default.Home,
            contentDescription = null,
            tint = if (isDeluxe) GoldAccent else PrimaryAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "B$number",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDeluxe) GoldAccent else Color.Black
        )
    }
}


/**
 * Cella interattiva della griglia.
 *
 * DESIGN CHOICE:
 * Utilizza una combinazione di colori pseudo-randomica basata sul cognome del cliente
 * per distinguere visivamente le prenotazioni adiacenti.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BungalowCellModern(
    booking: BungalowBooking?,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (backgroundColor, textColor) = when {
        booking == null -> Color.White to Color.Transparent
        else -> {
            val hash = Math.abs(booking.clientSurname?.hashCode() ?: 0)
            when (hash % 3) {
                0 -> BookingGreen to BookingGreenText
                1 -> BookingYellow to BookingYellowText
                else -> BookingBlue to BookingBlueText
            }
        }
    }

    Box(
        modifier = Modifier
            .size(width, height)
            .background(Color.White)
            .border(0.5.dp, GridLineColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (booking != null) {
            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = booking.clientSurname?.take(3)?.uppercase() ?: "",
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Cella dell'header dei giorni.
 */
@Composable
fun DayHeaderCellModern(text: String, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width, height)
            .border(0.5.dp, GridLineColor)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * FUNZIONE DI SUPPORTO: Ricerca prenotazione per data.
 * Normalizza il giorno selezionato e verifica se ricade nell'intervallo check-in/check-out.
 */
private fun findBungalowBooking(bookings: List<BungalowBooking>, bungalowId: String, day: Int, calendar: Calendar): BungalowBooking? {
    val targetDate = (calendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
    }.time
    return bookings.find { b ->
        if (b.bungalowId != bungalowId) return@find false
        val start = b.startDate?.toDate() ?: return@find false
        val end = b.endDate?.toDate() ?: return@find false
        !targetDate.before(start) && !targetDate.after(end)
    }
}