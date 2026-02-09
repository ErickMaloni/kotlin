package com.example.esame.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


/**
 * Schermata di Ricerca Globale dell'applicazione.
 *
 * SCOPO:
 * Fornire all'utente uno strumento per individuare rapidamente prenotazioni sia in
 * Spiaggia che nei Bungalow tramite ricerca testuale (nome o cognome).
 *
 * DESIGN CHOICES:
 * - **Unidirectional Data Flow (UDF)**: Il componente è "stateless". Riceve i dati dal chiamante
 *   (solitamente una Route) e comunica le interazioni tramite callback.
 * - **LazyColumn**: Ottimizzata per gestire liste potenzialmente lunghe di risultati senza
 *   impattare sulle performance di rendering.
 * - **Material Design 3**: Utilizzo di componenti Scaffold, TopAppBar e OutlinedTextField
 *   per un look moderno e coerente con le linee guida Android.
 *
 * @param searchText Il testo attualmente inserito nel campo di ricerca.
 * @param onSearchTextChanged Callback invocata ad ogni digitazione per aggiornare lo stato.
 * @param searchResults Lista dei risultati filtrati (Spiaggia e Bungalow).
 * @param onBackClick Callback per gestire il ritorno alla schermata precedente.
 * @param onResultClick Callback invocata quando l'utente seleziona un risultato specifico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    searchResults: List<SearchResult>, // <-- Tipo aggiornato
    onBackClick: () -> Unit,
    onResultClick: (SearchResult) -> Unit // <-- Callback aggiornata
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cerca Prenotazione") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Campo di testo per la ricerca
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChanged,
                label = { Text("Cerca nome o cognome (Spiaggia/Bungalow)...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista dei risultati
            if (searchText.isNotBlank() && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun risultato trovato per \"$searchText\"", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    if (searchResults.isNotEmpty()) {
                        item {
                            HeaderRow()
                            HorizontalDivider(thickness = 1.dp)
                        }
                    }
                    items(searchResults, key = { it.id + it.type.name }) { result ->
                        SearchResultListItem(
                            result = result,
                            onItemClick = { onResultClick(result) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

/**
 * Disegna una riga per un risultato di ricerca (Spiaggia o Bungalow).
 */
@Composable
fun SearchResultListItem(
    result: SearchResult,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona per distinguere visivamente Beach da Bungalow
        val icon = if (result.type == SearchType.BEACH) Icons.Default.BeachAccess else Icons.Default.Home
        val iconColor = if (result.type == SearchType.BEACH) Color(0xFF1976D2) else Color(0xFFF57C00)

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "${result.name} ${result.surname}".uppercase(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = result.location, // Già formattato come "Ombrellone: X" o "Bungalow: Y"
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(text = result.dateRange,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray) // <--- AGGIUNGI QUESTA RIGA
        }

        // Badge a destra per il tipo
        Surface(
            color = iconColor.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = if (result.type == SearchType.BEACH) "SPIAGGIA" else "BUNGALOW",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor
            )
        }
    }
}

/**
 * Disegna la riga di intestazione della lista.
 */
@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Cliente / Posizione", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("Tipo", fontWeight = FontWeight.Bold)
    }
}