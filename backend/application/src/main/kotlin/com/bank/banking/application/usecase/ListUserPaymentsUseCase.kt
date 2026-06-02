package com.bank.banking.application.usecase

import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CardPayment
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.PaymentHistoryRepository
import com.bank.banking.domain.port.SessionRepository

class ListUserPaymentsUseCase(
    private val sessions: SessionRepository,
    private val payments: PaymentHistoryRepository,
) {
    suspend fun execute(token: SessionToken, limit: Int = 50): List<CardPayment> {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        return payments.listByUser(session.userId, limit.coerceIn(1, 200))
    }
}
