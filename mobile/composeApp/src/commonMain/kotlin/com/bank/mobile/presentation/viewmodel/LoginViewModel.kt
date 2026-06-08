package com.bank.mobile.presentation.viewmodel

import com.bank.mobile.domain.usecase.LoginUseCase
import com.bank.mobile.presentation.session.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "woz@bank.com",
    val password: String = "Demo1234!",
    val error: String? = null,
    val loading: Boolean = false,
)

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val authSession: AuthSession,
    private val onLoginSuccess: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun clear() {
        scope.cancel()
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun login() {
        val current = _state.value
        if (current.loading) return
        scope.launch {
            _state.updateOnMain { it.copy(loading = true, error = null) }
            try {
                val token = loginUseCase(current.email, current.password)
                authSession.signIn(token)
                _state.updateOnMain { it.copy(loading = false, error = null) }
                onLoginSuccess()
            } catch (e: Throwable) {
                _state.updateOnMain {
                    it.copy(loading = false, error = e.message ?: "Login failed")
                }
            }
        }
    }
}
