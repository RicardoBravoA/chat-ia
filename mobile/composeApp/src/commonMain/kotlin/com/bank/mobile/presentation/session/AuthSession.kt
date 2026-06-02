package com.bank.mobile.presentation.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Portador de sesión para la capa de presentación. Las pantallas autenticadas obtienen el token con
 * [currentToken]; la navegación debe basarse en [sessionState], no en comprobar manualmente null.
 */
class AuthSession {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun signIn(token: String) {
        _sessionState.value = SessionState.Authenticated(token)
    }

    fun signOut() {
        _sessionState.value = SessionState.Unauthenticated
    }

    fun currentToken(): String? =
        (_sessionState.value as? SessionState.Authenticated)?.token
}
