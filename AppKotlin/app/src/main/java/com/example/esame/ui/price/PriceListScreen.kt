package com.example.esame.ui.price

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esame.data.model.PriceList
import com.example.esame.data.model.SubscriptionPrice
import com.example.esame.data.model.Week
import com.example.esame.data.model.BungalowPriceList
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// CONFIGURAZIONE COLORI
private val ModernBackground = Color(0xFFF5F7F9)
private val ModernSurface = Color.White
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)
private val AccentColor = Color(0xFF1976D2)
private val SeasonalYellow = Color(0xFFFFFDE7)
private val SeasonalOrange = Color(0xFFF57F17)
private val SeasonalBorder = Color(0xFFFBC02D).copy(alpha = 0.5f)



/**
 * Schermata principale per la gestione dei listini prezzi di Spiaggia e Bungalow.
 *
 * SCOPO:
 * Fornire un'interfaccia a tab che permetta di visualizzare, filtrare e modificare
 * tutti i prezzi della struttura.
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow (UDF)**: La Screen è "stateless". Riceve lo stato dal ViewModel
 *   e notifica gli eventi tramite callback, rendendo la UI prevedibile e testabile.
 * - **Sealed UiState**: La gestione dello stato tramite `when` (Loading, Error, Success)
 *   garantisce che l'interfaccia gestisca sempre tutti i possibili casi.
 * - **Side Effect Management**: Utilizza `LaunchedEffect` per gestire l'evento una-tantum
 *   del messaggio SnackBar, evitando che venga mostrato più volte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(
    uiState: PriceListUiState,
    onWeeklyPriceChange: (priceListId: String, weekKey: String, newWeekdayPrice: String, newHolidayPrice: String) -> Unit,
    onSubscriptionPriceChange: (subscriptionId: String, newPrice: String) -> Unit,
    onBeachFilterSelected: (String) -> Unit,
    onBungalowPriceChange: (priceListId: String, weekKey: String, typeKey: String, newPrice: String) -> Unit,
    onBungalowSeasonalPriceChange: (priceListId: String, typeKey: String, newPrice: String) -> Unit,
    onBungalowMonthSelected: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val successState = uiState as? PriceListUiState.Success

    if (successState != null) {
        successState.userMessage?.getContentIfNotHandled()?.let { message ->
            LaunchedEffect(snackbarHostState, message) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(ModernBackground)) {
                CenterAlignedTopAppBar(
                    title = { Text("Gestione Listini", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBackground)
                )

                PrimaryTabRow(
                    selectedTabIndex = successState?.selectedTab ?: 0,
                    containerColor = ModernBackground,
                    contentColor = AccentColor
                ) {
                    Tab(
                        selected = successState?.selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("Spiaggia", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = successState?.selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text("Bungalow", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        bottomBar = {
            if (successState != null) {
                Surface(
                    shadowElevation = 16.dp,
                    color = ModernSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Button(
                        onClick = onSaveClick,
                        enabled = !successState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (successState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("SALVA MODIFICHE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = ModernBackground
    ) { paddingValues ->
        when (uiState) {
            is PriceListUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentColor)
                }
            }
            is PriceListUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Errore nel caricamento dei dati.", color = TextPrimary)
                }
            }
            is PriceListUiState.Success -> {
                when (uiState.selectedTab) {
                    0 -> BeachPriceContent(uiState, paddingValues, onWeeklyPriceChange, onSubscriptionPriceChange, onBeachFilterSelected)
                    1 -> BungalowPriceContent(uiState, paddingValues, onBungalowPriceChange, onBungalowSeasonalPriceChange, onBungalowMonthSelected)
                }
            }
        }
    }
}

// SEZIONE SPIAGGIA
/**
 * Contenuto della tab "Spiaggia". Gestisce i filtri e la visualizzazione delle card dei prezzi.
 */
@Composable
private fun BeachPriceContent(
    state: PriceListUiState.Success,
    padding: PaddingValues,
    onWeeklyPriceChange: (String, String, String, String) -> Unit,
    onSubscriptionPriceChange: (String, String) -> Unit,
    onFilterSelected: (String) -> Unit
) {

    // Logica per derivare i filtri disponibili dai dati correnti
    val availableFilters = remember(state.beachPrices) {
        val months = state.beachPrices.filterIsInstance<PriceList>().mapNotNull { it.period?.uppercase() }.distinct()
        listOf("TUTTI", "STAGIONALE") + months
    }
    // Logica per filtrare e ordinare i prezzi da visualizzare
    val filteredPrices = remember(state.beachPrices, state.selectedBeachFilter) {
        val list = when (state.selectedBeachFilter) {
            "TUTTI" -> state.beachPrices
            "STAGIONALE" -> state.beachPrices.filterIsInstance<SubscriptionPrice>()
            else -> state.beachPrices.filter { (it as? PriceList)?.period?.uppercase() == state.selectedBeachFilter }
        }
        list.sortedWith(compareBy<com.example.esame.data.model.Price> { it is SubscriptionPrice }
            .thenBy { (it as? PriceList)?.weeks?.values?.minByOrNull { w -> w.startDate?.seconds ?: 0 }?.startDate?.seconds ?: 0 })
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScrollableTabRow(
            selectedTabIndex = availableFilters.indexOf(state.selectedBeachFilter).coerceAtLeast(0),
            containerColor = Color.Transparent,
            edgePadding = 16.dp, divider = {}, indicator = {}
        ) {
            availableFilters.forEach { filter ->
                FilterChip(
                    selected = state.selectedBeachFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter) },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentColor, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredPrices, key = { it.id }) { price ->
                when (price) {
                    is PriceList -> PriceListCard(price, onWeeklyPriceChange)
                    is SubscriptionPrice -> SubscriptionCard(price, onSubscriptionPriceChange)
                }
            }
        }
    }
}


/**
 * Card che rappresenta un listino mensile della spiaggia, con le relative settimane.
 */
@Composable
private fun PriceListCard(priceList: PriceList, onWeeklyPriceChange: (String, String, String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ModernSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val rowInfo = if (priceList.startRow != null && priceList.endRow != null) " [ FILE: ${priceList.startRow}-${priceList.endRow} ]" else ""
            Text(text = "${priceList.period?.uppercase() ?: ""}$rowInfo", fontWeight = FontWeight.Bold, color = AccentColor)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            val sortedWeeks = priceList.weeks?.toList()?.sortedBy { it.second.startDate?.seconds } ?: emptyList()
            sortedWeeks.forEach { (weekKey, week) ->
                WeeklyPriceRow(week = week, onPriceChange = { w, h -> onWeeklyPriceChange(priceList.id, weekKey, w, h) })
            }
        }
    }
}

/**
 * Card che rappresenta il prezzo dell'abbonamento stagionale della spiaggia.
 */
@Composable
private fun SubscriptionCard(subscription: SubscriptionPrice, onPriceChange: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SeasonalYellow),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(1.dp, SeasonalBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("✨ STAGIONALE SPIAGGIA", fontWeight = FontWeight.ExtraBold, color = SeasonalOrange, style = MaterialTheme.typography.titleMedium)
            Text(text = formatDateRange(subscription.startDate, subscription.endDate, "d MMMM yyyy"), style = MaterialTheme.typography.labelMedium, color = SeasonalOrange.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 16.dp))

            val priceVal = (subscription.price as? Number)?.toDouble() ?: 0.0
            CompactPriceTextField(
                label = "Prezzo Totale",
                price = if (priceVal == 0.0) "" else priceVal.toInt().toString(),
                onValueChange = { onPriceChange(subscription.id, it) }
            )
        }
    }
}


/**
 * Riga che mostra i campi di input per il prezzo feriale e festivo di una settimana.
 */
@Composable
private fun WeeklyPriceRow(week: Week, onPriceChange: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatDateRange(week.startDate, week.endDate, "dd MMM"), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PriceTextField("Feriale", week.weekdayPrice?.toString() ?: "", { onPriceChange(it, week.holidayPrice?.toString() ?: "") }, Modifier.weight(1f))
            PriceTextField("Festivo", week.holidayPrice?.toString() ?: "", { onPriceChange(week.weekdayPrice?.toString() ?: "", it) }, Modifier.weight(1f))
        }
    }
}


// SEZIONE LISTINO PREZZI BUNGALOW
/**
 * Contenuto della tab "Bungalow".
 */
@Composable
private fun BungalowPriceContent(
    state: PriceListUiState.Success,
    padding: PaddingValues,
    onBungalowPriceChange: (String, String, String, String) -> Unit,
    onBungalowSeasonalPriceChange: (String, String, String) -> Unit,
    onMonthSelected: (String) -> Unit
) {
    // Logica per l'ordinamento corretto dei mesi
    val monthOrder = listOf("GENNAIO", "FEBBRAIO", "MARZO", "APRILE", "MAGGIO", "GIUGNO", "LUGLIO", "AGOSTO", "SETTEMBRE", "OTTOBRE", "NOVEMBRE", "DICEMBRE", "MAY", "JUNE", "JULY", "AUGUST")

    // Logica per derivare i filtri disponibili dai dati correnti
    val availableMonths = remember(state.bungalowPrices) {
        val months = state.bungalowPrices
            .map { it.period.uppercase() }
            .filter { it != "SEASONAL" }
            .distinct()
            .sortedBy { m -> monthOrder.indexOf(m).takeIf { it != -1 } ?: 99 }
        listOf("TUTTI", "STAGIONALE") + months
    }

    // Logica per filtrare e ordinare i listini da visualizzare
    val filteredBungalows = remember(state.bungalowPrices, state.selectedBungalowMonth) {
        val selectedFilter = state.selectedBungalowMonth.uppercase()
        val baseList = when (selectedFilter) {
            "TUTTI" -> state.bungalowPrices
            "STAGIONALE" -> state.bungalowPrices.filter { it.period.uppercase() == "SEASONAL" }
            else -> state.bungalowPrices.filter { it.period.uppercase() == selectedFilter }
        }
        baseList.sortedWith(
            compareBy<BungalowPriceList> { it.period.uppercase() == "SEASONAL" }
                .thenBy { monthOrder.indexOf(it.period.uppercase()).takeIf { i -> i != -1 } ?: 99 }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScrollableTabRow(
            selectedTabIndex = availableMonths.indexOf(state.selectedBungalowMonth.uppercase()).coerceAtLeast(0),
            containerColor = Color.Transparent, edgePadding = 16.dp, divider = {}, indicator = {}
        ) {
            availableMonths.forEach { month ->
                FilterChip(
                    selected = state.selectedBungalowMonth.uppercase() == month,
                    onClick = { onMonthSelected(month) },
                    label = { Text(month) },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentColor, selectedLabelColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(filteredBungalows, key = { it.id }) { item ->
                if (item.period.uppercase() == "SEASONAL") {
                    BungalowSeasonalCard(item, onBungalowSeasonalPriceChange)
                } else {
                    BungalowPriceCard(item, onBungalowPriceChange)
                }
            }
        }
    }
}

/**
 * Card che rappresenta un listino mensile dei bungalow.
 */
@Composable
private fun BungalowPriceCard(priceList: BungalowPriceList, onChange: (String, String, String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ModernSurface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(priceList.period.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AccentColor)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))

            val sortedWeeks = priceList.weeks?.toList()?.sortedBy { it.second.startDate?.seconds } ?: emptyList()
            sortedWeeks.forEach { (weekKey, week) ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Surface(color = AccentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(text = "Settimana: ${formatDateRange(week.startDate, week.endDate, "dd/MM")}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge, color = AccentColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))

                    val types = week.price.keys.sorted()
                    types.chunked(2).forEach { rowTypes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowTypes.forEach { type ->
                                PriceTextField(
                                    label = type.replaceFirstChar { it.titlecase() },
                                    price = week.price[type]?.toString() ?: "",
                                    onValueChange = { newValue -> onChange(priceList.id, weekKey, type, newValue) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (weekKey != sortedWeeks.last().first) HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
            }
        }
    }
}


/**
 * Card che rappresenta il listino stagionale dei bungalow.
 */
@Composable
private fun BungalowSeasonalCard(priceList: BungalowPriceList, onPriceChange: (String, String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SeasonalYellow),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(1.dp, SeasonalBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("✨ STAGIONALE BUNGALOW", fontWeight = FontWeight.ExtraBold, color = SeasonalOrange, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            val prices = priceList.price ?: emptyMap()
            val types = prices.keys.sorted()

            types.chunked(2).forEach { rowTypes ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowTypes.forEach { type ->
                        CompactPriceTextField(
                            label = type.replaceFirstChar { it.titlecase() },
                            price = prices[type]?.toString() ?: "",
                            onValueChange = { onPriceChange(priceList.id, type, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// COMPONENTI UI RIUTILIZZABILI

/**
 * Campo di testo ottimizzato per l'inserimento di prezzi.
 */
@Composable
private fun PriceTextField(label: String, price: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = price,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("€ ", color = AccentColor, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
        )
    }
}


/**
 * Campo di testo compatto, ideale per le card stagionali.
 */
@Composable
private fun CompactPriceTextField(label: String, price: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SeasonalOrange.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = price,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("€ ", color = SeasonalOrange, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SeasonalBorder,
                focusedBorderColor = SeasonalOrange,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = TextPrimary)
        )
    }
}

/**
 * FUNZIONE HELPER per formattare un intervallo di date.
 */
private fun formatDateRange(start: Timestamp?, end: Timestamp?, pattern: String): String {
    if (start == null || end == null) return ""
    val sdf = SimpleDateFormat(pattern, Locale.ITALIAN)
    return "${sdf.format(start.toDate())} - ${sdf.format(end.toDate())}"
}