package com.bank.mobile.presentation.viewmodel

import com.bank.mobile.domain.model.Balance
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.usecase.GetBalanceUseCase
import com.bank.mobile.domain.usecase.GetPaymentsUseCase
import com.bank.mobile.presentation.session.AuthSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val balanceLabel: String = "--",
    val userNickname: String = "",
    val movements: List<Payment> = emptyList(),
    val movementsLoading: Boolean = false,
    val movementsError: String? = null,
)

class HomeViewModel(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val getPaymentsUseCase: GetPaymentsUseCase,
    private val authSession: AuthSession,
    private val formatMoney: (Double, String) -> String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun clear() {
        refreshJob?.cancel()
        scope.cancel()
    }

    fun refreshHomeData() {
        val token = authSession.currentToken() ?: return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _state.updateOnMain {
                it.copy(movementsLoading = true, movementsError = null)
            }
            val (balanceResult, movementsResult) = fetchHomeSnapshot(token)
            balanceResult.onSuccess { balance: Balance ->
                _state.updateOnMain {
                    it.copy(
                        balanceLabel = formatMoney(balance.amount, balance.currency),
                        userNickname = balance.nickname,
                    )
                }
            }
            movementsResult
                .onSuccess { rows ->
                    _state.updateOnMain {
                        it.copy(movements = rows, movementsLoading = false)
                    }
                }
                .onFailure { e ->
                    _state.updateOnMain {
                        it.copy(
                            movementsLoading = false,
                            movementsError = e.message ?: "Error cargando movimientos",
                        )
                    }
                }
        }
    }

    private suspend fun fetchHomeSnapshot(
        token: String,
    ): Pair<Result<Balance>, Result<List<Payment>>> = coroutineScope {
        val balanceDeferred = async { runCatching { getBalanceUseCase(token) } }
        val movementsDeferred = async {
            runCatching {
                getPaymentsUseCase(token).sortedByDescending { it.occurredAtEpochMs }
            }
        }
        balanceDeferred.await() to movementsDeferred.await()
    }
}
