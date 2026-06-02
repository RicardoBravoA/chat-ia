package com.bank.banking.application.usecase

import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardMovement
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.SessionRepository

class ListCreditCardMovementsUseCase(
    private val sessions: SessionRepository,
    private val cards: CreditCardRepository,
) {
    suspend fun execute(token: SessionToken, cardId: CreditCardId, limit: Int = 50): List<CreditCardMovement> {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        cards.findByIdAndUser(cardId, session.userId) ?: throw NotFoundException("Card not found")
        return cards.listMovements(cardId, session.userId, limit.coerceIn(1, 200))
    }
}
