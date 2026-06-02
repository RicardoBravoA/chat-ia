package com.bank.mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.bank.mobile.presentation.session.SessionState

/**
 * Efectos de navegación ligados al **ciclo de vida de la sesión**, no al token en bruto.
 *
 * Patrón: un solo lugar aplica transiciones de grafo cuando la sesión pasa entre
 * [SessionState.Unauthenticated] y [SessionState.Authenticated], evitando duplicar
 * `if (token != null)` en pantallas y manteniendo side-effects de navegación fuera de los ViewModels.
 */
@Composable
fun SessionNavigationEffect(
    sessionState: SessionState,
    navController: NavHostController,
) {
    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> {
                if (navController.currentBackStackEntry?.destination?.route == BankNavRoutes.Login) {
                    navController.navigate(BankNavRoutes.Home) {
                        popUpTo(BankNavRoutes.Login) { inclusive = true }
                    }
                }
            }
            SessionState.Unauthenticated -> {
                if (navController.currentBackStackEntry?.destination?.route != BankNavRoutes.Login) {
                    navController.navigate(BankNavRoutes.Login) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
