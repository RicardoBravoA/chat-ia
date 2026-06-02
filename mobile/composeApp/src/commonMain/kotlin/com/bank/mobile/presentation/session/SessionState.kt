package com.bank.mobile.presentation.session

/**
 * Estado de sesión de la aplicación (fuente de verdad para “¿hay usuario autenticado?”).
 * Sustituye comprobar `token != null` disperso: la navegación y las pantallas reaccionan a este modelo.
 */
sealed interface SessionState {
    data object Unauthenticated : SessionState

    data class Authenticated(val token: String) : SessionState
}
