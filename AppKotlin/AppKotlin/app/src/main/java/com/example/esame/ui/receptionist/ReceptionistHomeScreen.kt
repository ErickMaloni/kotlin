package com.example.esame.ui.receptionist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esame.R

/**
 * ReceptionistHomeScreen è la Dashboard principale per il ruolo Receptionist.
 *
 * SCOPO:
 * Mostrare una panoramica in tempo reale della struttura (Situazione Odierna)
 * e fornire l'accesso rapido alle aree di gestione (Spiaggia, Bungalow, Prezzi).
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow (UDF)**: Lo stato viene ricevuto tramite [uiState]
 *   e le azioni vengono notificate tramite callback.
 * - **LazyColumn**: Utilizzato come contenitore principale per supportare lo scroll
 *   su schermi di diverse dimensioni.
 * - **Separazione visiva**: Le card sono divise in sezioni logiche ("Situazione Odierna" vs "Gestione Struttura").
 * - **Feedback Visuale**: Utilizzo di icone Outlined e sfondi pastello per un look moderno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionistHomeScreen(
    uiState: ReceptionistHomeUiState,
    onMappaSpiaggiaClick: () -> Unit,
    onMappaBungalowClick: () -> Unit,
    onListinoPrezziClick: () -> Unit,
    onLogout: () -> Unit,
    onSearchClick: () -> Unit
) {
    // Sfondo
    val backgroundColor = Color(0xFFF8F9FA)

    // Stati per i BottomSheet
    var showArrivalsSheet by remember { mutableStateOf(false) }
    var showDeparturesSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = backgroundColor
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE3F2FD), backgroundColor)
                    )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // 1. HEADER MODERNO
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = Color(0xFFD3E6F8)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.padding(10.dp).fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Bentornato,", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                                Text(uiState.userSurname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row {
                            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, null) }
                            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, null, tint = Color.Red) }
                        }
                    }
                }

                // DASHBOARD (SITUAZIONE ODIERNA)
                item {
                    SectionTitle(title = "Situazione Odierna", icon = Icons.Outlined.Leaderboard)
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Card Ombrelloni
                        DashboardStatCard(
                            title = "Ombrelloni Liberi",
                            mainValue = "${uiState.freeUmbrellas}",
                            subValue = "/ ${uiState.totalUmbrellas}",
                            icon = Icons.Outlined.BeachAccess,
                            iconColor = Color(0xFF0288D1)
                        )

                        // Card Bungalow
                        DashboardStatCard(
                            title = "Bungalow Liberi",
                            mainValue = "${uiState.freeBungalows}",
                            subValue = "/ ${uiState.totalBungalows}",
                            icon = Icons.Outlined.Home,
                            iconColor = Color(0xFFF57C00)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                DashboardStatCardSmall(
                                    title = "Arrivi",
                                    value = uiState.arrivals.toString(),
                                    icon = Icons.Outlined.FlightLand,
                                    iconColor = Color(0xFF43A047),
                                    onClick = { showArrivalsSheet = true }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DashboardStatCardSmall(
                                    title = "Partenze",
                                    value = uiState.departures.toString(),
                                    icon = Icons.Outlined.FlightTakeoff,
                                    iconColor = Color(0xFFE53935),
                                    onClick = { showDeparturesSheet = true }
                                )
                            }
                        }
                    }
                }

                // NAVIGAZIONE (GESTIONE STRUTTURA)
                item {
                    SectionTitle(title = "Gestione Struttura", icon = Icons.Outlined.HomeWork)
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                TravelAppVerticalCard("Spiaggia", "Mappa Ombrelloni", R.drawable.tasto_spiaggia2, onMappaSpiaggiaClick)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TravelAppVerticalCard("Bungalow", "Mappa Alloggi", R.drawable.bungalowcamping, onMappaBungalowClick)
                            }
                        }
                        TravelAppHorizontalCard("Listino Prezzi", "Gestione tariffe 2025", "Update", R.drawable.pricelist, onListinoPrezziClick)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // Bottom Sheets per i movimenti unificati
        if (showArrivalsSheet) {
            MovementsBottomSheet(title = "Arrivi di Oggi", movements = uiState.arrivalsList, isArrival = true) { showArrivalsSheet = false }
        }
        if (showDeparturesSheet) {
            MovementsBottomSheet(title = "Partenze di Oggi", movements = uiState.departuresList, isArrival = false) { showDeparturesSheet = false }
        }
    }
}


/**
 * Visualizza il titolo di una sezione con icona e linea di separazione.
 */
@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
    }
}

/**
 * Card informativa grande per le statistiche principali.
 */
@Composable
fun DashboardStatCard(title: String, mainValue: String, subValue: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(mainValue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(subValue, style = MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                }
            }
        }
    }
}

/**
 * Card informativa compatta e cliccabile per i flussi (Arrivi/Partenze).
 */
@Composable
fun DashboardStatCardSmall(title: String, value: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}


/**
 * Bottom Sheet per la visualizzazione delle liste Arrivi/Partenze.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsBottomSheet(title: String, movements: List<String>, isArrival: Boolean, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 50.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
            if (movements.isEmpty()) {
                Text("Nessun movimento previsto.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(movements) { text ->
                        val bgColor = if (isArrival) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bgColor)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isArrival) Icons.Outlined.FlightLand else Icons.Outlined.FlightTakeoff, null, tint = if (isArrival) Color(0xFF2E7D32) else Color(0xFFC62828))
                                Spacer(Modifier.width(12.dp))
                                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelAppVerticalCard(title: String, subtitle: String, imageRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box {
            Image(painterResource(imageRes), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)), startY = 300f)))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Color.White.copy(0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TravelAppHorizontalCard(title: String, subtitle: String, priceTag: String, imageRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Surface(modifier = Modifier.size(76.dp), shape = RoundedCornerShape(16.dp)) {
                Image(painterResource(imageRes), null, contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
            Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)) {
                Text(priceTag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}