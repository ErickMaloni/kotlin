package com.example.esame.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.esame.ui.auth.LoginRoute
import com.example.esame.ui.auth.RegisterRoute
import com.example.esame.ui.bookings.BookingsRoute
import com.example.esame.ui.bungalowbookings.BungalowBookingsRoute
import com.example.esame.ui.price.PriceListRoute
import com.example.esame.ui.receptionist.ReceptionistHomeRoute
import com.example.esame.ui.search.SearchRoute

/**
 * Contenitore statico per le costanti delle rotte di navigazione.
 *
 * SCELTA TECNICA: L'uso di un oggetto con costanti evita errori di battitura (stringhe hardcoded)
 * in diverse parti dell'app e centralizza la gestione dei percorsi.
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val REGISTER = "register"
    const val MATRIX_PRENOTAZIONI = "matrix_prenotazioni"
    const val MAPPA_BUNGALOW = "mappa_bungalow"
    const val LISTINO_PREZZI = "listino_prezzi"
    const val SEARCH = "search"
    const val PROFILE_EDIT = "profile_edit"
}

/**
 * Punto centrale della navigazione dell'intera applicazione.
 *
 * SCOPO:
 * 1. Definire la gerarchia delle schermate (NavHost).
 * 2. Gestire il routing globale (Reindirizzamento Login/Home).
 * 3. Passare parametri tra le diverse schermate.
 *
 * DESIGN CHOICES:
 * - **LaunchedEffect**: Utilizzato per osservare lo stato dell'utente e reagire cambiando rotta.
 * - **Deep Linking Manuale**: La gestione dei parametri opzionali (?startDate=...) permette di
 *   collegare logicamente la prenotazione bungalow con quella della spiaggia.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // MainViewModel è iniettato qui per gestire lo stato dell'utente a livello globale
    val mainViewModel: MainViewModel = hiltViewModel()
    val userState by mainViewModel.uiState.collectAsState()

    /**
     * LOGICA DI NAVIGAZIONE REATTIVA
     * Questo blocco decide dove deve trovarsi l'utente in base alla sua autenticazione.
     * Se l'utente non è loggato, viene forzato al LOGIN.
     * Se è loggato e si trova su rotte "temporanee" (Splash/Login), viene mandato alla HOME.
     */
    LaunchedEffect(userState.user, userState.isLoading) {
        if (!userState.isLoading) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route

            if (userState.user == null) {
                // Utente non autenticato -> Vai al Login
                if (currentRoute != Routes.LOGIN) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true } // Pulisce lo stack
                        launchSingleTop = true
                    }
                }
            } else {
                // Utente autenticato -> Se è nel login, portalo a casa
                if (currentRoute == Routes.SPLASH || currentRoute == Routes.LOGIN) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        //  SCHERMATA INIZIALE / CARICAMENTO
        composable(Routes.SPLASH) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (userState.isLoading) CircularProgressIndicator()
            }
        }

        //  AUTENTICAZIONE
        composable(Routes.LOGIN) {
            LoginRoute(
                onLoginSuccess = { /* Gestito dal LaunchedEffect globale */ },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterRoute(
                onNavigateBack = { navController.popBackStack() },
                onRegistrationSuccess = { navController.popBackStack() }
            )
        }

        //  DASHBOARD PRINCIPALE
        composable(Routes.HOME) {
            ReceptionistHomeRoute(
                onMappaSpiaggiaClick = {
                    // Navigazione verso spiaggia con parametri di default (nessuna prenotazione bungalow attiva)
                    navController.navigate("${Routes.MATRIX_PRENOTAZIONI}?startDate=-1&bungalowBookingId=")
                },
                onMappaBungalowClick = { navController.navigate(Routes.MAPPA_BUNGALOW) },
                onListinoPrezziClick = { navController.navigate(Routes.LISTINO_PREZZI) },
                onLogout = { mainViewModel.onLogoutClick() },
                onSearchClick = { navController.navigate(Routes.SEARCH) }
            )
        }

        //  GESTIONE PRENOTAZIONI SPIAGGIA
        // Accetta parametri opzionali per collegare prenotazioni bungalow esistenti
        composable(
            route = "${Routes.MATRIX_PRENOTAZIONI}?startDate={startDate}&bungalowBookingId={bungalowBookingId}",
            arguments = listOf(
                navArgument("startDate") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("bungalowBookingId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val startDate = backStackEntry.arguments?.getLong("startDate") ?: -1L
            val bId = backStackEntry.arguments?.getString("bungalowBookingId") ?: ""

            BookingsRoute(
                initialDateMillis = startDate,
                bungalowBookingId = bId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // GESTIONE BUNGALOW
        composable(Routes.MAPPA_BUNGALOW) {
            BungalowBookingsRoute(
                onBackClick = { navController.popBackStack() },
                onNavigateToBeach = { startDate, _, bookingId ->
                    // Passaggio parametri: dalla selezione bungalow alla spiaggia
                    navController.navigate(
                        "${Routes.MATRIX_PRENOTAZIONI}?startDate=$startDate&bungalowBookingId=$bookingId"
                    )
                }
            )
        }

        // UTILS / ALTRE SCHERMATE
        composable(Routes.LISTINO_PREZZI) {
            PriceListRoute(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchRoute(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.PROFILE_EDIT) {
            Text("Schermata di Modifica Profilo (Placeholder)")
        }
    }
}