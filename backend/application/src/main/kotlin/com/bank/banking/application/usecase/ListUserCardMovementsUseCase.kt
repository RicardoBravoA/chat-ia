package com.bank.banking.application.usecase

import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CreditCardMovement
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.SessionRepository

class ListUserCardMovementsUseCase(
    private val sessions: SessionRepository,
    private val cards: CreditCardRepository,
) {
    suspend fun execute(token: SessionToken, limit: Int = 50): List<CreditCardMovement> {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        return cards.listMovementsByUser(session.userId, limit.coerceIn(1, 200))
    }
}
